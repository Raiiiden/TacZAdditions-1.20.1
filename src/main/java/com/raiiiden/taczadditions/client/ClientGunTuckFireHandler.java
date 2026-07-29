package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.api.event.common.GunShootEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

// Cancels client feedback for tucked shots the server will reject.
@Mod.EventBusSubscriber(modid = "taczadditions", value = Dist.CLIENT)
public final class ClientGunTuckFireHandler {

    private ClientGunTuckFireHandler() {
    }

    @SubscribeEvent
    public static void onGunShoot(GunShootEvent event) {
        if (event.getLogicalSide() != LogicalSide.CLIENT) return;
        if (GunTuckHandler.tuckProgress <= 0f) return;
        if (!TacZAdditionsConfig.COMMON.blockFireWhenTucked.get()) return;

        float threshold = TacZAdditionsConfig.COMMON.tuckFireBlockThreshold.get().floatValue();
        if (GunTuckHandler.tuckProgress >= threshold) {
            event.setCanceled(true);
        }
    }
}
