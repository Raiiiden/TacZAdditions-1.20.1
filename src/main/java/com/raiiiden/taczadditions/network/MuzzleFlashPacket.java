package com.raiiiden.taczadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

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
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                // Fix: Get the correct IntegerProperty for light level
                IntegerProperty LIGHT_LEVEL = (IntegerProperty) Blocks.LIGHT.getStateDefinition().getProperty("level");
                mc.level.setBlock(
                        msg.pos,
                        Blocks.LIGHT.defaultBlockState().setValue(LIGHT_LEVEL, msg.lightLevel),
                        3
                );
            }
        });
        contextSupplier.get().setPacketHandled(true);
    }
}
