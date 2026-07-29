package com.raiiiden.taczadditions.network;

import com.raiiiden.taczadditions.config.ConfigSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

// Carries the server's common config snapshot.
public class ConfigSnapshotPacket {
    public final Map<String, Object> values;

    public ConfigSnapshotPacket(Map<String, Object> values) {
        this.values = values;
    }

    public static ConfigSnapshotPacket ofLocalValues() {
        return new ConfigSnapshotPacket(ConfigSync.snapshotLocalValues());
    }

    public static void encode(ConfigSnapshotPacket msg, FriendlyByteBuf buffer) {
        ConfigSync.write(buffer, msg.values);
    }

    public static ConfigSnapshotPacket decode(FriendlyByteBuf buffer) {
        return new ConfigSnapshotPacket(ConfigSync.read(buffer));
    }

    public static void handle(ConfigSnapshotPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        // Direction is PLAY_TO_CLIENT, so this only ever runs on a client receiving a server's values.
        contextSupplier.get().enqueueWork(() -> ConfigSync.applyServerValues(msg.values));
        contextSupplier.get().setPacketHandled(true);
    }
}
