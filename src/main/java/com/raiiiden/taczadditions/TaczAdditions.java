package com.raiiiden.taczadditions;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.raiiiden.taczadditions.network.ModNetworking;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TaczAdditions.MODID)
public class TaczAdditions {
  public static final Logger LOGGER = LogManager.getLogger();
  public static final String MODID = "taczadditions";

  public TaczAdditions() {
    IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

    // Register particles
    ModParticles.PARTICLES.register(modBus);

    // Setup listeners
    modBus.addListener(this::clientSetup);
    modBus.addListener(this::commonSetup);
    TacZAdditionsConfig.registerConfigs();
  }

  private void clientSetup(FMLClientSetupEvent event) {
    // Client-specific
  }

  private void commonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(ModNetworking::registerPackets);
  }
}
