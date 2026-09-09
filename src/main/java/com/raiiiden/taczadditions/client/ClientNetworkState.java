package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.network.ModNetworking;
import net.minecraft.client.Minecraft;

public final class ClientNetworkState {
    private ClientNetworkState() {}

    public static boolean isServerModPresent() {
        var listener = Minecraft.getInstance().getConnection();
        return listener != null
                && ModNetworking.CHANNEL.isRemotePresent(listener.getConnection());
    }
}
