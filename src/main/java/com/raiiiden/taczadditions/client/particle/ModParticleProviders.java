package com.raiiiden.taczadditions.client.particle;

import com.raiiiden.taczadditions.TaczAdditions;
import com.raiiiden.taczadditions.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TaczAdditions.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModParticleProviders {

    @SubscribeEvent
    public static void registerProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.WATER_PUFF.get(), WaterPuffParticle.Provider::new);
        event.registerSpriteSet(ModParticles.WATER_DROP.get(), WaterDropletParticle.Provider::new);
        event.registerSpriteSet(ModParticles.WATER_BUBBLE.get(), WaterTrailBubbleParticle.Provider::new);
    }

    private ModParticleProviders() {}
}
