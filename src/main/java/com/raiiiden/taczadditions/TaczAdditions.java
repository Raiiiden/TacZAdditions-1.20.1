package com.raiiiden.taczadditions;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TaczAdditions.MODID)
public class TaczAdditions {
  public static final String MODID = "taczadditions";

  public TaczAdditions() {
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
    TacZAdditionsConfig.registerConfigs();
  }

  private void clientSetup(FMLClientSetupEvent event) {
  }
}