package com.raiiiden.taczadditions.pip;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.compat.oculus.OculusCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

@OnlyIn(Dist.CLIENT)
public final class PipScopeRenderer {

    private static float currentZoom = 1.0f;

    // The magnification with the tick staircase filtered out, before the aiming ramp is applied.
    private static float smoothedMagnification = 1.0f;
    private static long lastZoomNanos;

    // How far behind the authored magnification the filter is allowed to sit. Long enough to bridge
    // the gap between two game ticks, short enough that the lens does not feel rubbery.
    private static final float ZOOM_TIME_CONSTANT = 0.045f;
    // A frame long enough to have been a pause is integrated as one step, so a stall cannot make the
    // lens leap.
    private static final float MAX_ZOOM_STEP_SECONDS = 0.05f;

    // FAKE mode state
    private static int captureTexId = -1;
    private static int captureWidth = 0;
    private static int captureHeight = 0;
    private static boolean fakeUseMainRt = false;

    // REAL mode state (no-shader path)
    private static boolean isRenderingPip = false;

    private static final int   PIP_MIN_WIDTH        = 480;
    private static final int   PIP_MIN_HEIGHT       = 270;

    private static int pipFboId     = -1;
    private static int pipTexId     = -1;
    private static int pipFboWidth  = 0;
    private static int pipFboHeight = 0;

    // The shader PIP is a complete second Oculus frame, so its persistent GPU history is snapshotted
    // around that pass and its alternate FOV never reaches the normal frame's TAA or SSBO history.
    private static boolean isolateShaderPipeline = false;
    private static ShaderHistorySnapshot shaderHistorySnapshot;
    private static ShaderHistorySnapshot pipShaderHistorySnapshot;

    // Temporal reuse state
    private static float lastCameraYaw   = Float.MAX_VALUE;
    private static float lastCameraPitch = Float.MAX_VALUE;
    private static float lastZoom        = -1f;
    private static int   frameCounter    = 0;

    private static final float ANGLE_THRESHOLD = 0.03f;
    // A magnification sweep settles in increments far below a hundredth, so a coarse threshold here
    // freezes the lens for the whole tail of a scroll while the rest of the frame keeps moving.
    private static final float ZOOM_THRESHOLD  = 0.0005f;

    // Shader (Iris/Oculus) deferred PIP state. Draw calls inside renderLevel go through Iris' gbuffer
    // pipeline, so the reticle is skipped there and replayed in Pass 3 through a fresh buffer source.
    private static boolean shaderPipPending = false;

    // Snapshot FBO allocated lazily in renderShaderPip()
    private static int shaderCaptureTexId  = -1;
    private static int shaderCaptureFboId  = -1;
    private static int shaderCaptureWidth  = 0;
    private static int shaderCaptureHeight = 0;

    // One deferred overlay per ocular lens, holding its fan geometry, the matrices it was drawn with
    // and the writers that replay the eyepiece surround and reticle, so composite scopes all draw.
    private static final class DeferredOverlay {
        final float cx, cy, cz, rad;
        final Runnable ocularWriter;   // opaque eyepiece surround, drawn first
        final Runnable divisionWriter; // reticle, drawn last
        final Matrix4f proj;
        final Matrix4f modelView;

        DeferredOverlay(float cx, float cy, float cz, float rad,
                        Runnable ocularWriter, Runnable divisionWriter,
                        Matrix4f proj, Matrix4f modelView) {
            this.cx = cx; this.cy = cy; this.cz = cz; this.rad = rad;
            this.ocularWriter   = ocularWriter;
            this.divisionWriter = divisionWriter;
            this.proj      = new Matrix4f(proj);
            this.modelView = new Matrix4f(modelView);
        }
    }

    private static final java.util.List<DeferredOverlay> shaderOverlays = new java.util.ArrayList<>();

    private PipScopeRenderer() {}

    public static float getCurrentZoom() { return currentZoom; }
    public static boolean isRenderingPip() { return isRenderingPip; }

    // True once the magnification is high enough to be worth a whole second pass. Below it the crop of
    // the frame already drawn is close enough to tell apart, and costs nothing.
    public static boolean useSecondPass() {
        return PipScope.mode() == PipScopeMode.REAL
                && currentZoom >= TacZAdditionsConfig.CLIENT.pipScopeRealMinMagnification.get();
    }

    // The magnification the lens is about to be drawn at, recomputed from live state: the stored value
    // is a frame behind here, and is filtered on elapsed time so a sweep ramps instead of stepping.
    public static void refreshZoom(float partialTick) {
        long now = System.nanoTime();
        float step = lastZoomNanos == 0L ? 0f : (now - lastZoomNanos) / 1.0e9f;
        lastZoomNanos = now;
        step = Mth.clamp(step, 0f, MAX_ZOOM_STEP_SECONDS);

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.getCameraEntity() != player) {
            currentZoom = 1f;
            smoothedMagnification = 1f;
            return;
        }

        ItemStack stack = KeepingItemRenderer.getRenderer().getCurrentItem();
        if (!(stack.getItem() instanceof IGun gun)) {
            currentZoom = 1f;
            smoothedMagnification = 1f;
            return;
        }

        // Magnification is multiplicative, so the filter runs on its logarithm; otherwise the same
        // filter would feel slow at the low end of a range and abrupt at the high end.
        float magnification = Math.max(1f, gun.getAimingZoom(stack));
        if (smoothedMagnification <= 0f || step <= 0f) {
            smoothedMagnification = magnification;
        } else {
            double blend = 1.0 - Math.exp(-step / ZOOM_TIME_CONSTANT);
            double from = Math.log(smoothedMagnification);
            double to = Math.log(magnification);
            smoothedMagnification = (float) Math.exp(from + (to - from) * blend);
            if (Math.abs(smoothedMagnification - magnification) < 1.0e-4f) {
                smoothedMagnification = magnification;
            }
        }

        // The aiming ramp is already evaluated per frame, so it is applied after the filter rather
        // than through it, which keeps raising the gun as crisp as TaCZ makes it.
        float aimingProgress = IClientPlayerGunOperator.fromLocalPlayer(player)
                .getClientAimingProgress(partialTick);
        currentZoom = 1f + (smoothedMagnification - 1f) * aimingProgress;
    }

    public static boolean needsRealPipRender() {
        if (!useSecondPass()) return false;
        // Below this the lens shows the world at its normal scale, which the ordinary frame already
        // drew, so there is nothing for a second pass to add.
        if (currentZoom <= 1.001f) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        // Nothing is looking through the lens from out here, so the pass would be drawn and dropped.
        if (!mc.options.getCameraType().isFirstPerson()) return false;

        if (pipTexId == -1) { frameCounter++; return true; } // always render first frame

        if (TacZAdditionsConfig.CLIENT.pipScopeRefreshEveryFrame.get()) {
            frameCounter++;
            return true;
        }

        float yaw   = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        boolean moved  = Math.abs(yaw - lastCameraYaw)   > ANGLE_THRESHOLD
                || Math.abs(pitch - lastCameraPitch) > ANGLE_THRESHOLD;
        boolean zoomed = Math.abs(currentZoom - lastZoom)  > ZOOM_THRESHOLD;
        boolean alt    = (frameCounter % 2) == 0;
        frameCounter++;
        return moved || zoomed || alt;
    }

    // Where the cropped image starts, on both axes. It has to be centred on the screen centre, since
    // narrowing the field of view is exactly a scale about the camera axis and nothing else.
    private static float cropOrigin(float zoom) {
        return 0.5f * (1f - 1f / zoom);
    }

    private static int pipWidth(int screenWidth) {
        double scale = TacZAdditionsConfig.CLIENT.pipScopeResolutionScale.get();
        return Math.max(PIP_MIN_WIDTH, (int) (screenWidth * scale));
    }

    private static int pipHeight(int screenHeight) {
        double scale = TacZAdditionsConfig.CLIENT.pipScopeResolutionScale.get();
        return Math.max(PIP_MIN_HEIGHT, (int) (screenHeight * scale));
    }

    public static void beginRealPipRender() {
        isRenderingPip = true;
        isolateShaderPipeline = OculusCompat.isUsingRenderPack();
    }

    public static boolean isIsolatingShaderPipeline() {
        return isRenderingPip && isolateShaderPipeline;
    }

    public static void beginShaderHistoryIsolation(ShaderHistorySnapshot snapshot) {
        if (shaderHistorySnapshot != null) shaderHistorySnapshot.close();
        shaderHistorySnapshot = snapshot;

        // Replace the live pipeline resources with the scope's own history bank. On the first scope
        // frame no bank exists yet, so the previous-matrix isolation below covers it.
        if (pipShaderHistorySnapshot != null) {
            pipShaderHistorySnapshot.restore();
            pipShaderHistorySnapshot = null;
        }
    }

    public static void finishShaderHistoryIsolation(ShaderHistorySnapshot pipSnapshot) {
        if (pipShaderHistorySnapshot != null) pipShaderHistorySnapshot.close();
        pipShaderHistorySnapshot = pipSnapshot;
        restoreShaderHistorySnapshot();
    }

    public static void restoreShaderHistorySnapshot() {
        if (shaderHistorySnapshot == null) return;
        try {
            shaderHistorySnapshot.restore();
        } finally {
            shaderHistorySnapshot = null;
        }
    }

    // Restore the normal target and state if the recursive secondary render fails.
    public static void abortRealPipRender() {
        if (!isRenderingPip) return;
        restoreShaderHistorySnapshot();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        isRenderingPip = false;
        isolateShaderPipeline = false;
    }

    private static void recordCameraState() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        lastCameraYaw   = mc.player.getYRot();
        lastCameraPitch = mc.player.getXRot();
        lastZoom        = currentZoom;
    }

    private static void ensurePipFbo(int srcW, int srcH) {
        int dstW = pipWidth(srcW);
        int dstH = pipHeight(srcH);
        if (pipFboId != -1 && pipFboWidth == dstW && pipFboHeight == dstH) return;

        if (pipTexId != -1) GL11.glDeleteTextures(pipTexId);
        if (pipFboId != -1) GL30.glDeleteFramebuffers(pipFboId);

        pipTexId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, pipTexId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB8, dstW, dstH, 0,
                GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        pipFboId = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, pipFboId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, pipTexId, 0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        pipFboWidth  = dstW;
        pipFboHeight = dstH;
    }

    private static void ensureShaderCaptureFbo(int w, int h) {
        if (shaderCaptureFboId != -1 && shaderCaptureWidth == w && shaderCaptureHeight == h) return;

        if (shaderCaptureTexId != -1) GL11.glDeleteTextures(shaderCaptureTexId);
        if (shaderCaptureFboId != -1) GL30.glDeleteFramebuffers(shaderCaptureFboId);

        shaderCaptureTexId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, shaderCaptureTexId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB8, w, h, 0,
                GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        shaderCaptureFboId = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, shaderCaptureFboId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, shaderCaptureTexId, 0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        shaderCaptureWidth  = w;
        shaderCaptureHeight = h;
    }

    public static void captureAndClearForNormalRender() {
        RenderSystem.assertOnRenderThread();
        // Normally restored by the Oculus finalize hook. Keep this as a safety
        // net before the normal frame begins.
        restoreShaderHistorySnapshot();
        Minecraft mc = Minecraft.getInstance();
        mc.getMainRenderTarget().bindWrite(false);

        int srcW = mc.getMainRenderTarget().width;
        int srcH = mc.getMainRenderTarget().height;
        int dstW = pipWidth(srcW);
        int dstH = pipHeight(srcH);

        ensurePipFbo(srcW, srcH);

        int mainFboId = mc.getMainRenderTarget().frameBufferId;
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainFboId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, pipFboId);
        GL30.glBlitFramebuffer(0, 0, srcW, srcH, 0, 0, dstW, dstH,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        mc.getMainRenderTarget().bindWrite(true);
        // Iris' final pass leaves depth writes disabled, and glClear respects the write masks, so
        // clearing without normalizing them leaves the zoomed depth behind and the world leaks.
        RenderSystem.disableScissor();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.stencilMask(0xFF);
        RenderSystem.clearColor(0f, 0f, 0f, 1f);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT,
                Minecraft.ON_OSX);

        recordCameraState();
        isRenderingPip = false;
        isolateShaderPipeline = false;
    }

    // Grabs the world exactly as the frame left it, before any of the held model has been drawn.
    // Later would fold the gun into its own lens, since the crop is centred where a raised scope sits.
    public static void captureFrameIfNeeded() {
        if (isRenderingPip) return;
        if (!PipScope.enabled()) return;
        if (useSecondPass()) return;
        captureCurrentFrame();
    }

    public static void captureCurrentFrame() {
        RenderSystem.assertOnRenderThread();
        // Under a shader pack the deferred replay blits the composited frame for itself; there is no
        // gun-free frame to grab here. See PipScope#mode.
        if (OculusCompat.isUsingRenderPack()) {
            fakeUseMainRt = true;
            return;
        }
        fakeUseMainRt = false;

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getMainRenderTarget().width;
        int h = mc.getMainRenderTarget().height;

        if (captureTexId == -1 || captureWidth != w || captureHeight != h) {
            if (captureTexId != -1) GL11.glDeleteTextures(captureTexId);
            captureTexId = GL11.glGenTextures();
            captureWidth = w;
            captureHeight = h;
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, captureTexId);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB8, w, h, 0,
                    GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
        } else {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, captureTexId);
        }

        // Set every grab rather than only on creation, so changing the setting takes effect without
        // waiting for the texture to be rebuilt by a resize.
        int filter = TacZAdditionsConfig.CLIENT.pipScopeFakeSmoothSampling.get()
                ? GL11.GL_LINEAR
                : GL11.GL_NEAREST;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);

        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    // Called from BedrockAttachmentModel once per ocular lens under shaders. Stores the raw gun-space
    // fan, the matrices active at draw time, and the writers that replay the eyepiece and reticle.
    public static void setShaderDeferredOverlay(
            float fanCenterX, float fanCenterY, float fanCenterZ,
            float fanRad,
            Runnable ocularWriter,
            Runnable divisionWriter,
            Matrix4f savedProj, Matrix4f savedModelView) {
        shaderOverlays.add(new DeferredOverlay(
                fanCenterX, fanCenterY, fanCenterZ, fanRad,
                ocularWriter, divisionWriter,
                savedProj, savedModelView));
        // Always replay under shaders: even at 1x the ocular mask and reticle must still be drawn,
        // which the zoom gate in renderZoomedView would otherwise skip.
        shaderPipPending = true;
    }

    // No-shader PIP draw: a full-screen textured quad clipped to the aperture stencil. The shader
    // path does not use this, it defers to renderShaderPip() via setShaderDeferredOverlay().
    public static void renderZoomedView(int stencilRef) {
        if (isRenderingPip) return;
        PipScopeMode mode = PipScope.mode();
        if (mode == PipScopeMode.OFF) return;
        float zoom = currentZoom;
        if (zoom <= 1.001f) return;

        // ── No-shader paths ───────────────────────────────────────────────────
        int texId;
        float u0, u1, v0, v1;

        // Below the second pass threshold there is no re-rendered frame to show, so the crop stands
        // in for it. The two look alike at low magnification, which is the point of the threshold.
        if (useSecondPass()) {
            if (pipTexId == -1) return;
            texId = pipTexId;
            u0 = 0f; u1 = 1f; v0 = 0f; v1 = 1f;
        } else {
            if (fakeUseMainRt) {
                texId = Minecraft.getInstance().getMainRenderTarget().getColorTextureId();
            } else {
                if (captureTexId == -1) return;
                texId = captureTexId;
            }
            u0 = cropOrigin(zoom); u1 = u0 + 1f / zoom;
            v0 = u0;               v1 = v0 + 1f / zoom;
        }

        RenderSystem.assertOnRenderThread();

        RenderSystem.stencilFunc(GL11.GL_EQUAL, stencilRef, 0xFF);
        RenderSystem.stencilMask(0x00);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texId);

        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        PoseStack mvStack  = RenderSystem.getModelViewStack();
        mvStack.pushPose();
        mvStack.last().pose().identity();
        mvStack.last().normal().identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);

        Matrix4f id = new Matrix4f();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.vertex(id, -1f, -1f, 0f).uv(u0, v0).endVertex();
        buf.vertex(id,  1f, -1f, 0f).uv(u1, v0).endVertex();
        buf.vertex(id,  1f,  1f, 0f).uv(u1, v1).endVertex();
        buf.vertex(id, -1f,  1f, 0f).uv(u0, v1).endVertex();
        BufferUploader.drawWithShader(buf.end());

        RenderSystem.setProjectionMatrix(savedProj, VertexSorting.ORTHOGRAPHIC_Z);
        mvStack.popPose();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.stencilMask(0xFF);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    // Called at the RETURN of GameRenderer.renderLevel, once Iris has written the final frame.
    // Rebuilds the aperture in the main target's own stencil, then draws the world, eyepiece and reticle.
    public static void renderShaderPip() {
        if (!shaderPipPending) return;
        shaderPipPending = false;

        // Snapshot + clear the overlay list so the next frame starts fresh even
        // if this method throws midway.
        final java.util.List<DeferredOverlay> overlays = new java.util.ArrayList<>(shaderOverlays);
        shaderOverlays.clear();
        if (overlays.isEmpty()) return;


        RenderSystem.assertOnRenderThread();
        Minecraft mc = Minecraft.getInstance();

        // --- source texture (shared by every lens this frame) ---
        final int srcTexId;
        final boolean realMode = useSecondPass() && pipTexId != -1;

        if (realMode) {
            srcTexId = pipTexId;
        } else {
            ensureShaderCaptureFbo(
                    mc.getMainRenderTarget().width,
                    mc.getMainRenderTarget().height
            );
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,
                    mc.getMainRenderTarget().frameBufferId);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, shaderCaptureFboId);
            GL30.glBlitFramebuffer(
                    0, 0, mc.getMainRenderTarget().width, mc.getMainRenderTarget().height,
                    0, 0, shaderCaptureWidth, shaderCaptureHeight,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST
            );
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            srcTexId = shaderCaptureTexId;
        }

        mc.getMainRenderTarget().bindWrite(true);

        // FAKE-mode magnified crop half-window (REAL uses the full re-rendered frame).
        final float inv = 1f / Math.max(currentZoom, 1.0001f);
        final int CIRCLE_REF = 0xFE; // ~1 & 0xFF — the INVERT of stencil value 1

        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        PoseStack mvStack = RenderSystem.getModelViewStack();
        mvStack.pushPose();

        // Clear depth (so the composited scene doesn't cull our overlays) and the
        // stencil (we rebuild the aperture mask in it below).
        RenderSystem.depthMask(true);
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
        GL11.glEnable(GL11.GL_STENCIL_TEST);

        for (DeferredOverlay ov : overlays) {
            Matrix4f mvp = new Matrix4f(ov.proj).mul(ov.modelView);

            // The cropped window sits on the camera axis, same as the re-rendered frame does by
            // construction. See cropOrigin.
            final float srcU0 = realMode ? 0f : cropOrigin(Math.max(currentZoom, 1.0001f));
            final float srcV0 = srcU0;
            final float dU    = realMode ? 1f : inv;
            final float dV    = realMode ? 1f : inv;

            // ── Pass A: stencil = 1 over the ocular mesh (no colour) ───────────
            applyOverlayMatrices(mvStack, ov);
            RenderSystem.stencilMask(0xFF);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.depthMask(false);
            if (ov.ocularWriter != null) ov.ocularWriter.run();
            RenderSystem.colorMask(true, true, true, true);


            // ── Pass B: INVERT the aperture circle within the ocular (1 -> 0xFE)
            applyOverlayMatrices(mvStack, ov);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INVERT);
            RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            {
                BufferBuilder b = Tesselator.getInstance().getBuilder();
                b.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                b.vertex(ov.cx, ov.cy, ov.cz).color(255, 255, 255, 255).endVertex();
                for (int j = 0; j <= 90; j++) {
                    float a = j * (float) (Math.PI * 2.0) / 90f;
                    b.vertex(ov.cx + Mth.cos(a) * ov.rad, ov.cy + Mth.sin(a) * ov.rad, ov.cz)
                            .color(255, 255, 255, 255).endVertex();
                }
                BufferUploader.drawWithShader(b.end());
            }
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            RenderSystem.stencilMask(0x00); // colour passes below must not touch stencil


            // Pass C: zoomed world (textured fan) where stencil == 0xFE, with u and v taken from
            // the NDC position across the source rect, on a bottom-origin GL texture.
            applyOverlayMatrices(mvStack, ov);
            RenderSystem.stencilFunc(GL11.GL_EQUAL, CIRCLE_REF, 0xFF);
            RenderSystem.disableBlend();
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, srcTexId);
            {
                BufferBuilder fan = Tesselator.getInstance().getBuilder();
                fan.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_TEX);
                {
                    Vector4f v = mvp.transform(new Vector4f(ov.cx, ov.cy, ov.cz, 1f));
                    float u  = srcU0 + (v.x / v.w * 0.5f + 0.5f) * dU;
                    float tv = srcV0 + (v.y / v.w * 0.5f + 0.5f) * dV;
                    fan.vertex(ov.cx, ov.cy, ov.cz).uv(u, tv).endVertex();
                }
                for (int j = 0; j <= 90; j++) {
                    float a = j * (float) (Math.PI * 2.0) / 90f;
                    float vx = ov.cx + Mth.cos(a) * ov.rad;
                    float vy = ov.cy + Mth.sin(a) * ov.rad;
                    Vector4f v = mvp.transform(new Vector4f(vx, vy, ov.cz, 1f));
                    float u  = srcU0 + (v.x / v.w * 0.5f + 0.5f) * dU;
                    float tv = srcV0 + (v.y / v.w * 0.5f + 0.5f) * dV;
                    fan.vertex(vx, vy, ov.cz).uv(u, tv).endVertex();
                }
                BufferUploader.drawWithShader(fan.end());
            }


            // ── Pass D: opaque eyepiece surround where stencil == 1 (the gap) ──
            if (ov.ocularWriter != null) {
                applyOverlayMatrices(mvStack, ov);
                RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
                ov.ocularWriter.run();
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }


            // ── Pass E: reticle where stencil == 0xFE (inside the lens) ────────
            if (ov.divisionWriter != null) {
                applyOverlayMatrices(mvStack, ov);
                RenderSystem.stencilFunc(GL11.GL_EQUAL, CIRCLE_REF, 0xFF);
                ov.divisionWriter.run();
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }
        }

        // ── Restore ───────────────────────────────────────────────────────────
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        RenderSystem.stencilMask(0xFF);
        RenderSystem.setProjectionMatrix(savedProj, VertexSorting.ORTHOGRAPHIC_Z);
        mvStack.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        mc.getMainRenderTarget().bindWrite(true);
    }

    // Restore the exact matrices an overlay's geometry was drawn with.
    private static void applyOverlayMatrices(PoseStack mvStack, DeferredOverlay ov) {
        mvStack.last().pose().set(ov.modelView);
        mvStack.last().normal().identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(ov.proj), VertexSorting.ORTHOGRAPHIC_Z);
    }

    public static void cleanup() {
        fakeUseMainRt              = false;
        shaderPipPending           = false;
        isolateShaderPipeline      = false;
        isRenderingPip             = false;
        if (shaderHistorySnapshot != null) {
            shaderHistorySnapshot.close();
            shaderHistorySnapshot = null;
        }
        if (pipShaderHistorySnapshot != null) {
            pipShaderHistorySnapshot.close();
            pipShaderHistorySnapshot = null;
        }
        shaderOverlays.clear();

        if (captureTexId != -1) {
            GL11.glDeleteTextures(captureTexId);
            captureTexId = -1;
            captureWidth = 0;
            captureHeight = 0;
        }
        if (pipTexId != -1) {
            GL11.glDeleteTextures(pipTexId);
            pipTexId = -1;
        }
        if (pipFboId != -1) {
            GL30.glDeleteFramebuffers(pipFboId);
            pipFboId = -1;
        }
        if (shaderCaptureTexId != -1) {
            GL11.glDeleteTextures(shaderCaptureTexId);
            shaderCaptureTexId = -1;
        }
        if (shaderCaptureFboId != -1) {
            GL30.glDeleteFramebuffers(shaderCaptureFboId);
            shaderCaptureFboId = -1;
        }
        pipFboWidth  = 0;
        pipFboHeight = 0;
        shaderCaptureWidth  = 0;
        shaderCaptureHeight = 0;
        lastCameraYaw   = Float.MAX_VALUE;
        lastCameraPitch = Float.MAX_VALUE;
        lastZoom        = -1f;
        frameCounter    = 0;
        currentZoom     = 1f;
        smoothedMagnification = 1f;
        lastZoomNanos   = 0L;
    }
}
