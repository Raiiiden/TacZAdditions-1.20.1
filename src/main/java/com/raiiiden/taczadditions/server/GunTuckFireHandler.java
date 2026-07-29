package com.raiiiden.taczadditions.server;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.api.event.common.GunShootEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

// Cancels tucked shots before TaCZ spawns a bullet.
@Mod.EventBusSubscriber
public final class GunTuckFireHandler {

    private GunTuckFireHandler() {
    }

    @SubscribeEvent
    public static void onGunShoot(GunShootEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) return;
        if (!TacZAdditionsConfig.COMMON.blockFireWhenTucked.get()) return;

        float threshold = TacZAdditionsConfig.COMMON.tuckFireBlockThreshold.get().floatValue();
        if (ServerGunTuckTracker.getProgress(event.getShooter()) >= threshold) {
            event.setCanceled(true);
        }
    }
}
