package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.TaczAdditions;
import com.raiiiden.taczadditions.config.ConfigSync;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Restores local common settings after leaving a server.
@Mod.EventBusSubscriber(modid = TaczAdditions.MODID, value = Dist.CLIENT)
public final class ClientConfigSyncEvents {
    private ClientConfigSyncEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ConfigSync.releaseServerValues();
        FreeAimHandler.reset();
    }
}
