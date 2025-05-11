package com.raiiiden.taczadditions.network;

import com.raiiiden.taczadditions.client.renderer.MuzzleFlashRenderer;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MuzzleFlashPacket {
    private final BlockPos pos;
    private final int lightLevel;

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
        contextSupplier.get().enqueueWork(() -> {
            if (!TacZAdditionsConfig.SERVER.enableMuzzleFlash.get()) {
                return;
            }

            MuzzleFlashRenderer.triggerFlashAt(msg.pos, msg.lightLevel);
        });
        contextSupplier.get().setPacketHandled(true);
    }
}
