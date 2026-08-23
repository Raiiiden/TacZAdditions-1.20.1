package com.raiiiden.taczadditions.registry;

import com.raiiiden.taczadditions.TaczAdditions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, TaczAdditions.MODID);

    // Spray thrown off the water surface. No new art: it animates through vanilla's own generic
    // puff sprites, the ones the explosion particle uses, so it sits in the vanilla look by default.
    public static final RegistryObject<SimpleParticleType> WATER_PUFF =
            PARTICLE_TYPES.register("water_puff", () -> new SimpleParticleType(false));

    // Droplet thrown out of the middle of the splash. Vanilla's splash droplet art, our own arc.
    public static final RegistryObject<SimpleParticleType> WATER_DROP =
            PARTICLE_TYPES.register("water_drop", () -> new SimpleParticleType(false));

    // Trail bubble. Vanilla's bubble art, but it can draw through the water surface so the trail is
    // visible from above, which the vanilla bubble cannot do.
    public static final RegistryObject<SimpleParticleType> WATER_BUBBLE =
            PARTICLE_TYPES.register("water_bubble", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
