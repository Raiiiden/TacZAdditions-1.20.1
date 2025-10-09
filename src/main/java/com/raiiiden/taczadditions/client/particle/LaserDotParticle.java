package com.raiiiden.taczadditions.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class LaserDotParticle extends TextureSheetParticle {
    protected LaserDotParticle(ClientLevel level, double x, double y, double z, int color) {
        super(level, x, y, z);
        this.lifetime = 1;
        this.gravity = 0;
        this.hasPhysics = false;
        this.alpha = 1.0F;
        this.quadSize = 0.08F;

        // Extract RGB from color int
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        // Use brightness multiplier but clamp to prevent oversaturation
        float brightness = 1.8F;
        this.rCol = Math.min(r * brightness, 1.0F);
        this.gCol = Math.min(g * brightness, 1.0F);
        this.bCol = Math.min(b * brightness, 1.0F);

        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        // Always return full brightness, ignoring world lighting
        return 15728880;
    }

    @Override
    public void tick() {
        // Remove immediately on first tick
        this.remove();
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;
        public Factory(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            // Color is passed as the vx parameter (we reuse it since we don't need velocity)
            int color = (int) vx;
            LaserDotParticle p = new LaserDotParticle(world, x, y, z, color);
            p.pickSprite(spriteSet);
            return p;
        }
    }
}