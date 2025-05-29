package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.api.event.common.GunFireEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientGunFireHandler {

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        if (!(event.getShooter() instanceof LocalPlayer)) return;

        float x = TacZAdditionsConfig.CLIENT.recoilVisualX.get().floatValue();
        float y = TacZAdditionsConfig.CLIENT.recoilVisualY.get().floatValue();
        float z = TacZAdditionsConfig.CLIENT.recoilVisualZ.get().floatValue();

        float xActual = (float) ((Math.random() * 2.0 - 1.0) * x);
        float yActual = y;
        float zActual = z;

        GunRecoilHandler.trigger(xActual, yActual, zActual);
    }
}
