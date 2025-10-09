package com.raiiiden.taczadditions;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, TaczAdditions.MODID);

    public static final RegistryObject<SimpleParticleType> LASER_DOT =
            PARTICLES.register("laser_dot", () -> new SimpleParticleType(true));
}