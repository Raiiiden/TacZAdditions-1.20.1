package com.raiiiden.taczadditions.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

// Water spray. Built the same way Superb Warfare builds its splash cloud: a small puff that runs
// through vanilla's generic sprite sheet and damps out fast, rather than a new texture.
public class WaterPuffParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    WaterPuffParticle(ClientLevel level, double x, double y, double z,
                      double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;

        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        this.friction = 0.88F;
        this.gravity = 0.3F;
        this.hasPhysics = false;

        this.lifetime = 7 + this.random.nextInt(7);
        this.quadSize = 0.09F + this.random.nextFloat() * 0.07F;
        this.setSize(0.2F, 0.2F);
        this.setSpriteFromAge(sprites);
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

        this.setSpriteFromAge(this.sprites);
        this.yd -= 0.04D * this.gravity;
        this.move(this.xd, this.yd, this.zd);

        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;

        this.alpha = 1.0F - (float) this.age / this.lifetime;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double xd, double yd, double zd) {
            return new WaterPuffParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}
