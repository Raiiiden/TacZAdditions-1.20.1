package com.raiiiden.taczadditions;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.raiiiden.taczadditions.network.ModNetworking;
import com.raiiiden.taczadditions.server.GunFireLightManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
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

    ModParticles.PARTICLES.register(modBus);

    modBus.addListener(this::clientSetup);
    modBus.addListener(this::commonSetup);
    TacZAdditionsConfig.registerConfigs();
  }

  private void clientSetup(FMLClientSetupEvent event) {
    boolean hasSodiumDL  = ModList.get().isLoaded("sodiumdynamiclights");
    boolean hasAtomicDL  = ModList.get().isLoaded("dynamiclights");

    LOGGER.info("[TacZAdditions] DL detection — sodiumdynamiclights={}, dynamiclights={}", hasSodiumDL, hasAtomicDL);

    if (hasSodiumDL) {
      LOGGER.info("[TacZAdditions] Initializing SodiumDLAdapter");
      com.raiiiden.taczadditions.client.SodiumDLAdapter.init();
    } else if (hasAtomicDL) {
      MinecraftForge.EVENT_BUS.register(
          com.raiiiden.taczadditions.client.ClientGunFireLightManager.class);
    }
  }

  private void commonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(ModNetworking::registerPackets);

    // Atomicstryker DL server path: only register the server-tick handler when present.
    if (ModList.get().isLoaded("dynamiclights")) {
      MinecraftForge.EVENT_BUS.register(GunFireLightManager.class);
    }
  }
}
