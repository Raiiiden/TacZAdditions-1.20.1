package com.raiiiden.taczadditions.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class RecoilRenderHandler {

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        long timeSince = System.currentTimeMillis() - GunRecoilHandler.lastRecoilTime;
        if (timeSince > 150) return;

        float strength = 1.0f - (timeSince / 150f);

        event.setYaw(event.getYaw() + GunRecoilHandler.recoilX * strength);   // left/right
        event.setPitch(event.getPitch() + GunRecoilHandler.recoilY * strength); // up/down
        event.setRoll(event.getRoll() + GunRecoilHandler.recoilZ * strength);   // tilt
    }
}
