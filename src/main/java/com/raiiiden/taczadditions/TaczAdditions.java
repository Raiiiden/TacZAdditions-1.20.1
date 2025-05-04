package com.raiiiden.taczadditions;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.raiiiden.taczadditions.network.ModNetworking;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TaczAdditions.MODID)
public class TaczAdditions {
  public static final String MODID = "taczadditions";

  public TaczAdditions() {
    var modBus = FMLJavaModLoadingContext.get().getModEventBus();
    modBus.addListener(this::clientSetup);
    modBus.addListener(this::commonSetup);

    // configs
    TacZAdditionsConfig.registerConfigs();

  }

  private void clientSetup(FMLClientSetupEvent event) {
    // Client-specific
  }

  private void commonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(ModNetworking::registerPackets);
  }
}
