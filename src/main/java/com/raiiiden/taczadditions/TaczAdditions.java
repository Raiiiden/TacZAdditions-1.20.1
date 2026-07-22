package com.raiiiden.taczadditions;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.raiiiden.taczadditions.network.ModNetworking;
import com.raiiiden.taczadditions.registry.ModSounds;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TaczAdditions.MODID)
public class TaczAdditions {
  public static final Logger LOGGER = LogManager.getLogger();
  public static final String MODID = "taczadditions";

  public TaczAdditions() {
    IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

    modBus.addListener(this::clientSetup);
    modBus.addListener(this::commonSetup);
    modBus.addListener(this::onConfigReload);
    ModSounds.SOUND_EVENTS.register(modBus);
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

    // Muzzle flash dynamic lights are client-side only. The server only sends packets
    // and uses block lights when dynamic light packets are not available.
  }

  private void onConfigReload(ModConfigEvent.Reloading event) {
    if (event.getConfig().getSpec() != TacZAdditionsConfig.SERVER_SPEC) return;

    var server = ServerLifecycleHooks.getCurrentServer();
    if (server == null) return;

    for (var player : server.getPlayerList().getPlayers()) {
      ModNetworking.sendLaserToggleConfig(player);
    }
  }
}
