package com.raiiiden.taczadditions.pip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.client.model.bedrock.BedrockCube;
import com.tacz.guns.client.model.bedrock.BedrockCubeBox;
import com.tacz.guns.client.model.bedrock.BedrockCubePerFace;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.compat.oculus.OculusCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.List;

// Draws the lens aperture and what shows through it, replacing the ocular pass TaCZ runs while a
// scope is in first person. Under a shader pack the overlay waits until the composite is done.
@OnlyIn(Dist.CLIENT)
public final class PipLensRenderer {

    // The aperture radius comes from the ocular mesh, so scopes already differ from one another.
    // This is the single global nudge for when every scope reads too wide or too narrow.
    private static final float LENS_RADIUS_CALIBRATION = 1.0f;

    // Lets the caller hand back the model's own part renderer without exposing it.
    @FunctionalInterface
    public interface TempPartRenderer {
        void render(PoseStack poseStack, ItemDisplayContext transformType, RenderType renderType,
                    int light, int overlay, List<BedrockPart> path);
    }

    // The lens circle, in the same space the aperture fan is drawn in.
    private record OcularGeometry(float cx, float cy, float cz, float radius) {
    }

    private PipLensRenderer() {
    }

    public static void render(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType,
                              int light, int overlay, boolean selective,
                              List<List<BedrockPart>> ocularNodePaths, List<Boolean> isScopeOcular,
                              List<List<BedrockPart>> divisionNodePaths, float scopeViewRadiusModifier,
                              TempPartRenderer tempParts) {
        if (ocularNodePaths.isEmpty()) {
            return;
        }

        boolean shaderDefer = OculusCompat.isUsingRenderPack();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();

        // invert a circular aperture into each ocular's stencil region.
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INVERT);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();

        float aimingProgress = 1f;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            aimingProgress = IClientPlayerGunOperator.fromLocalPlayer(player)
                    .getClientAimingProgress(Minecraft.getInstance().getFrameTime());
        }

        OcularGeometry[] geometries = new OcularGeometry[ocularNodePaths.size()];
        for (int i = 0; i < ocularNodePaths.size(); i++) {
            if (!selective || isScopeOcular.get(i)) {
                geometries[i] = computeOcularGeometry(matrixStack, ocularNodePaths.get(i), scopeViewRadiusModifier);
            }
        }

        for (int i = 0; i < ocularNodePaths.size(); i++) {
            if (selective && !isScopeOcular.get(i)) {
                continue;
            }
            OcularGeometry geometry = geometries[i];
            float radius = geometry.radius() * aimingProgress;
            RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
            builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            builder.vertex(geometry.cx(), geometry.cy(), geometry.cz()).color(255, 255, 255, 255).endVertex();
            for (int segment = 0; segment <= 90; segment++) {
                float angle = segment * ((float) Math.PI * 2F) / 90.0F;
                float sin = Mth.sin(angle);
                float cos = Mth.cos(angle);
                builder.vertex(geometry.cx() + cos * radius, geometry.cy() + sin * radius, geometry.cz())
                        .color(255, 255, 255, 255).endVertex();
            }
            BufferUploader.drawWithShader(builder.end());
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

        // put the magnified world into the aperture, or hand it to the deferred replay.
        Matrix4f capturedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f capturedModelView = new Matrix4f(RenderSystem.getModelViewMatrix());

        for (int i = 0; i < ocularNodePaths.size(); i++) {
            if (selective && !isScopeOcular.get(i)) {
                continue;
            }

            OcularGeometry geometry = geometries[i];
            float radius = geometry.radius() * aimingProgress;
            int circleRef = ~(i + 1) & 0xFF;

            if (shaderDefer) {
                Matrix4f savedPose = new Matrix4f(matrixStack.last().pose());
                Matrix3f savedNormal = new Matrix3f(matrixStack.last().normal());
                List<BedrockPart> divisionPath = i < divisionNodePaths.size() ? divisionNodePaths.get(i) : null;
                Runnable ocularWriter = makeDeferredMeshWriter(
                        ocularNodePaths.get(i), savedPose, savedNormal,
                        transformType, renderType, light, overlay);
                Runnable divisionWriter = makeDeferredMeshWriter(
                        divisionPath, savedPose, savedNormal,
                        transformType, renderType, light, overlay);

                PipScopeRenderer.setShaderDeferredOverlay(
                        geometry.cx(), geometry.cy(), geometry.cz(), radius,
                        ocularWriter, divisionWriter,
                        capturedProjection, capturedModelView);
            } else {
                PipScopeRenderer.renderZoomedView(circleRef);
            }
        }

        // the eyepiece and the reticle, unless the shader path has to replay them later.
        for (int i = 0; i < ocularNodePaths.size() && i < divisionNodePaths.size(); i++) {
            if (i > Byte.MAX_VALUE) {
                throw new IllegalArgumentException("Index of oculus is out of range for 127");
            }
            if (shaderDefer) {
                continue;
            }
            if (selective && !isScopeOcular.get(i)) {
                RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
                tempParts.render(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i));
            } else {
                RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
                tempParts.render(matrixStack, transformType, renderType, light, overlay, ocularNodePaths.get(i));
                int invertedRef = ~(i + 1) & 0xFF;
                RenderSystem.stencilFunc(GL11.GL_EQUAL, invertedRef, 0xFF);
                tempParts.render(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i));
            }
        }
    }

    // Iris can flush geometry that was only ever meant to write stencil, so under a shader pack the
    // ocular meshes leave the normal draw entirely and the deferred overlay replays them instead.
    public static void hideOcularLeaves(List<List<BedrockPart>> ocularNodePaths) {
        for (List<BedrockPart> path : ocularNodePaths) {
            if (path != null && !path.isEmpty()) {
                path.get(path.size() - 1).visible = false;
            }
        }
    }

    // Replays one mesh leaf after the shader composite, from the matrices it was drawn with.
    @Nullable
    private static Runnable makeDeferredMeshWriter(@Nullable List<BedrockPart> path,
                                                   Matrix4f savedPose, Matrix3f savedNormal,
                                                   ItemDisplayContext transformType, RenderType renderType,
                                                   int light, int overlay) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        return () -> {
            PoseStack temp = new PoseStack();
            temp.last().pose().set(savedPose);
            temp.last().normal().set(savedNormal);
            for (int i = 0; i < path.size() - 1; i++) {
                path.get(i).translateAndRotateAndScale(temp);
            }
            BedrockPart leaf = path.get(path.size() - 1);
            boolean previousVisibility = leaf.visible;
            leaf.visible = true;
            // A fresh buffer source, so this is a plain vanilla batch that Iris does not intercept.
            BufferBuilder freshBuilder = new BufferBuilder(256);
            MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(freshBuilder);
            VertexConsumer consumer = bufferSource.getBuffer(renderType);
            leaf.render(temp, transformType, consumer, light, overlay);
            bufferSource.endBatch();
            leaf.visible = previousVisibility;
        };
    }

    // The lens circle taken from the ocular mesh itself, projected into the space the fan is drawn
    // in. Deriving it from geometry is what lets one setting suit every scope.
    private static OcularGeometry computeOcularGeometry(PoseStack poseStack, List<BedrockPart> path,
                                                        float scopeViewRadiusModifier) {
        poseStack.pushPose();
        for (BedrockPart part : path) {
            part.translateAndRotateAndScale(poseStack);
        }
        Matrix4f pose = poseStack.last().pose();
        BedrockPart leaf = path.get(path.size() - 1);

        // The centre and depth are the values TaCZ already calibrated the aperture against.
        final float scale = 16f * 90f;
        final float cx = pose.m30() * scale;
        final float cy = pose.m31() * scale;
        final float cz = -90f;

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;
        for (BedrockCube cube : leaf.cubes) {
            float cubeMinX;
            float cubeMinY;
            float cubeMinZ;
            float cubeMaxX;
            float cubeMaxY;
            float cubeMaxZ;
            if (cube instanceof BedrockCubeBox box) {
                cubeMinX = box.minX;
                cubeMinY = box.minY;
                cubeMinZ = box.minZ;
                cubeMaxX = box.maxX;
                cubeMaxY = box.maxY;
                cubeMaxZ = box.maxZ;
            } else if (cube instanceof BedrockCubePerFace perFace) {
                cubeMinX = perFace.minX;
                cubeMinY = perFace.minY;
                cubeMinZ = perFace.minZ;
                cubeMaxX = perFace.maxX;
                cubeMaxY = perFace.maxY;
                cubeMaxZ = perFace.maxZ;
            } else {
                continue;
            }
            minX = Math.min(minX, cubeMinX);
            maxX = Math.max(maxX, cubeMaxX);
            minY = Math.min(minY, cubeMinY);
            maxY = Math.max(maxY, cubeMaxY);
            minZ = Math.min(minZ, cubeMinZ);
            maxZ = Math.max(maxZ, cubeMaxZ);
        }

        float radius;
        if (minX > maxX) {
            // Nothing usable in the mesh, so fall back to the fixed radius TaCZ uses.
            radius = 80f * scopeViewRadiusModifier;
        } else {
            // Cube bounds are authored in sixteenths, which is what compile() divides by.
            float localCenterX = (minX + maxX) * 0.5f / 16f;
            float localCenterY = (minY + maxY) * 0.5f / 16f;
            float localCenterZ = (minZ + maxZ) * 0.5f / 16f;
            float localRadiusX = (maxX - minX) * 0.5f / 16f;
            float localRadiusY = (maxY - minY) * 0.5f / 16f;
            Vector4f center = new Vector4f(localCenterX, localCenterY, localCenterZ, 1f).mul(pose);
            Vector4f edgeX = new Vector4f(localCenterX + localRadiusX, localCenterY, localCenterZ, 1f).mul(pose);
            Vector4f edgeY = new Vector4f(localCenterX, localCenterY + localRadiusY, localCenterZ, 1f).mul(pose);
            float rx = (float) Math.hypot(edgeX.x() - center.x(), edgeX.y() - center.y());
            float ry = (float) Math.hypot(edgeY.x() - center.x(), edgeY.y() - center.y());
            radius = (rx + ry) * 0.5f * scale * LENS_RADIUS_CALIBRATION;
        }

        poseStack.popPose();
        return new OcularGeometry(cx, cy, cz, radius);
    }
}
