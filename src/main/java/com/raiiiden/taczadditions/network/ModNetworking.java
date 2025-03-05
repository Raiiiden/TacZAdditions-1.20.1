package com.raiiiden.taczadditions.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkDirection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.function.Supplier;

public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("taczadditions", "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void registerPackets() {
        int id = 0;
        CHANNEL.registerMessage(
                id++,
                MuzzleFlashPacket.class,
                MuzzleFlashPacket::encode,
                MuzzleFlashPacket::decode,
                MuzzleFlashPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void sendMuzzleFlash(Player player, BlockPos pos, int lightLevel) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new MuzzleFlashPacket(pos, lightLevel));
    }
}
