package com.raiiiden.taczadditions.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;

// Water thrown out of the middle of a splash: it climbs, arcs over and falls back out, and is gone
// the moment it rejoins the water. Vanilla's splash droplet art, but the arc is ours, because the
// vanilla splash particle discards whatever velocity it is handed.
public class WaterDropletParticle extends TextureSheetParticle {

    WaterDropletParticle(ClientLevel level, double x, double y, double z,
                         double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.pickSprite(sprites);

        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        this.gravity = 1.0F;
        this.friction = 0.995F;
        this.hasPhysics = true;

        this.lifetime = 12 + this.random.nextInt(14);
        this.quadSize = 0.05F + this.random.nextFloat() * 0.045F;
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

        this.yd -= 0.04D * this.gravity;
        this.move(this.xd, this.yd, this.zd);

        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;

        if (this.onGround) {
            this.xd *= 0.6D;
            this.zd *= 0.6D;
        }

        // Back in the water: it has merged with the surface again.
        if (this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).is(FluidTags.WATER)) {
            this.remove();
            return;
        }

        float life = (float) this.age / this.lifetime;
        this.alpha = 1.0F - Mth.clamp((life - 0.7F) / 0.3F, 0.0F, 1.0F);
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
            return new WaterDropletParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}
