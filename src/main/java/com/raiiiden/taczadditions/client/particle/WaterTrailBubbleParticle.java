package com.raiiiden.taczadditions.client.particle;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FogType;

/**
 * The bubble trail a bullet drags through water. Vanilla's bubble texture and motion, with one
 * difference that is the whole reason it exists: the water surface does not hide it.
 *
 * <p>Minecraft draws translucent terrain before the particle pass, and water writes depth, so any
 * particle under the surface is depth-culled when the camera is above it. Vanilla bubbles simply
 * vanish when you look into water from outside. When the camera is out of the water this one draws
 * without the depth test so the trail stays readable from the bank; underwater, where the ordering
 * is not a problem, it behaves normally. Lighting is left alone, so the trail darkens with depth the
 * way everything else does, and the whole bypass can be switched off in the client config.</p>
 */
public class WaterTrailBubbleParticle extends TextureSheetParticle {

    // Depth test off, so the water surface in front cannot cull the bubble behind it.
    private static final ParticleRenderType THROUGH_WATER = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager manager) {
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "taczadditions:through_water";
        }
    };

    WaterTrailBubbleParticle(ClientLevel level, double x, double y, double z,
                             double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.pickSprite(sprites);

        // Same damping vanilla's bubble applies to the velocity it is handed.
        this.xd = xd * 0.2D + (Math.random() * 2.0D - 1.0D) * 0.02D;
        this.yd = yd * 0.2D + (Math.random() * 2.0D - 1.0D) * 0.02D;
        this.zd = zd * 0.2D + (Math.random() * 2.0D - 1.0D) * 0.02D;

        this.setSize(0.02F, 0.02F);
        // A touch larger than vanilla's, since it is being read through several blocks of water.
        this.quadSize *= this.random.nextFloat() * 0.6F + 0.5F;
        this.lifetime = (int) (12.0D / (Math.random() * 0.8D + 0.2D));
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.yd += 0.002D;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.85D;
        this.yd *= 0.85D;
        this.zd *= 0.85D;

        // Pops on reaching the surface, like every other bubble in the game.
        if (!this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).is(FluidTags.WATER)) {
            this.remove();
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        if (TacZAdditionsConfig.CLIENT.cullBubbleTrailBehindWater.get()) {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
        // Only skip the depth test for the case it is meant to solve: looking into the water from
        // outside it. With the camera underwater there is no surface in the way to begin with.
        return Minecraft.getInstance().gameRenderer.getMainCamera().getFluidInCamera() == FogType.WATER
                ? ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
                : THROUGH_WATER;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return new WaterTrailBubbleParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}
