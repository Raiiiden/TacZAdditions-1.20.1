package com.raiiiden.taczadditions;

import com.raiiiden.taczadditions.client.event.ClientTickHandler;
import com.raiiiden.taczadditions.client.event.GunFireListener;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TaczAdditions.MODID)
public class TaczAdditions {
  public static final String MODID = "taczadditions";

  public TaczAdditions() {
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
    TacZAdditionsConfig.registerConfigs();

    // Register event handlers
    MinecraftForge.EVENT_BUS.register(ClientTickHandler.class);
    MinecraftForge.EVENT_BUS.register(GunFireListener.class);
  }

  private void clientSetup(FMLClientSetupEvent event) {
  }
}
