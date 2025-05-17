package com.raiiiden.taczadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MuzzleFlashPacket {
    public final BlockPos pos;
    public final int lightLevel;

    public MuzzleFlashPacket(BlockPos pos, int lightLevel) {
        this.pos = pos;
        this.lightLevel = lightLevel;
    }

    public static void encode(MuzzleFlashPacket msg, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(msg.pos);
        buffer.writeInt(msg.lightLevel);
    }

    public static MuzzleFlashPacket decode(FriendlyByteBuf buffer) {
        return new MuzzleFlashPacket(buffer.readBlockPos(), buffer.readInt());
    }

    public static void handle(MuzzleFlashPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().setPacketHandled(true);
    }
}
