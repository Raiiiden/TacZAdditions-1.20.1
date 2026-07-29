package com.raiiiden.taczadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MuzzleFlashPacket {
    public final int entityId;
    public final int lightLevel;
    public final int color;

    public MuzzleFlashPacket(int entityId, int lightLevel, int color) {
        this.entityId = entityId;
        this.lightLevel = lightLevel;
        this.color = color;
    }

    public static void encode(MuzzleFlashPacket msg, FriendlyByteBuf buffer) {
        buffer.writeInt(msg.entityId);
        buffer.writeInt(msg.lightLevel);
        buffer.writeInt(msg.color);
    }

    public static MuzzleFlashPacket decode(FriendlyByteBuf buffer) {
        return new MuzzleFlashPacket(buffer.readInt(), buffer.readInt(), buffer.readInt());
    }

    public static void handle(MuzzleFlashPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        // Direction is PLAY_TO_CLIENT, so this only runs on the client — safe to reference client handler.
        contextSupplier.get().enqueueWork(() -> MuzzleFlashPacketClientHandler.handle(msg));
        contextSupplier.get().setPacketHandled(true);
    }
}
