package com.raiiiden.taczadditions;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TaczAdditions.MODID)
public class TaczAdditions {
  public static final String MODID = "taczadditions";

  public TaczAdditions() {
    // Register the client setup listener.
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
  }

  private void clientSetup(FMLClientSetupEvent event) {
    // Perform any client-specific setup here.
    // Aim key handling is managed by the AimKey class (registered as an EventBusSubscriber).
  }
}
