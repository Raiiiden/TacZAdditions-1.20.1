package com.raiiiden.taczadditions.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

 // The dot is drawn as a camera-facing billboard inside the level render pass, so it is recomputed
 // and redrawn every frame from the same partial tick — there is structurally zero latency between the
 // raytraced hit position and what is shown, regardless of framerate.
public final class LaserDotRenderer extends RenderType {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("taczadditions", "textures/particle/laser_dot.png");

    /** Brightness multiplier applied to the laser colour (matches the old particle look). */
    private static final float BRIGHTNESS = 1.8F;

    public static final RenderType LASER_DOT = RenderType.create(
            "taczadditions:laser_dot",
            DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                    .setTextureState(new TextureStateShard(TEXTURE, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setOverlayState(NO_OVERLAY)
                    .createCompositeState(false)
    );

    // Never instantiated; this type only exists so we can reach RenderStateShard's protected constants.
    private LaserDotRenderer(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                            boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        throw new UnsupportedOperationException();
    }

    public static void renderDot(PoseStack pose, MultiBufferSource buffers, Camera camera,
                                 double worldX, double worldY, double worldZ,
                                 int color, float alpha, float halfSize) {
        Vec3 cam = camera.getPosition();
        float rx = (float) (worldX - cam.x);
        float ry = (float) (worldY - cam.y);
        float rz = (float) (worldZ - cam.z);

        Quaternionf rotation = camera.rotation();
        Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F).rotate(rotation);
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F).rotate(rotation);

        int r = Math.min((int) ((color >> 16 & 0xFF) * BRIGHTNESS), 255);
        int g = Math.min((int) ((color >> 8 & 0xFF) * BRIGHTNESS), 255);
        int b = Math.min((int) ((color & 0xFF) * BRIGHTNESS), 255);
        int a = Math.max(0, Math.min((int) (alpha * 255.0F), 255));

        Matrix4f matrix = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(LASER_DOT);

        addVertex(vc, matrix, rx, ry, rz, right, up, -1.0F, -1.0F, halfSize, 0.0F, 1.0F, r, g, b, a);
        addVertex(vc, matrix, rx, ry, rz, right, up, -1.0F,  1.0F, halfSize, 0.0F, 0.0F, r, g, b, a);
        addVertex(vc, matrix, rx, ry, rz, right, up,  1.0F,  1.0F, halfSize, 1.0F, 0.0F, r, g, b, a);
        addVertex(vc, matrix, rx, ry, rz, right, up,  1.0F, -1.0F, halfSize, 1.0F, 1.0F, r, g, b, a);
    }

    private static void addVertex(VertexConsumer vc, Matrix4f matrix, float rx, float ry, float rz,
                                  Vector3f right, Vector3f up, float cornerX, float cornerY, float halfSize,
                                  float u, float v, int r, int g, int b, int a) {
        float px = rx + (right.x * cornerX + up.x * cornerY) * halfSize;
        float py = ry + (right.y * cornerX + up.y * cornerY) * halfSize;
        float pz = rz + (right.z * cornerX + up.z * cornerY) * halfSize;
        vc.vertex(matrix, px, py, pz).uv(u, v).color(r, g, b, a).endVertex();
    }
}