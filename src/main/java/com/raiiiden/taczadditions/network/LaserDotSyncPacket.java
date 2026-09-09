package com.raiiiden.taczadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class LaserDotSyncPacket {
    public final int entityId;
    public final double x;
    public final double y;
    public final double z;
    public final int color;

    public LaserDotSyncPacket(int entityId, double x, double y, double z, int color) {
        this.entityId = entityId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
    }

    public static void encode(LaserDotSyncPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.entityId);
        buffer.writeDouble(msg.x);
        buffer.writeDouble(msg.y);
        buffer.writeDouble(msg.z);
        buffer.writeInt(msg.color);
    }

    public static LaserDotSyncPacket decode(FriendlyByteBuf buffer) {
        return new LaserDotSyncPacket(
                buffer.readInt(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readInt()
        );
    }

    public static void handle(LaserDotSyncPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> LaserDotPacketClientHandler.handle(msg));
        contextSupplier.get().setPacketHandled(true);
    }
}
