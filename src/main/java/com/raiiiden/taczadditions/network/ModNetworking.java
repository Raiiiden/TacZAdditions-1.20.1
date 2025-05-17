package com.raiiiden.taczadditions.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkDirection;

import java.util.Optional;

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

    public static void sendMuzzleFlash(Player shooter, BlockPos pos, int lightLevel) {
        if (!(shooter instanceof ServerPlayer serverPlayer)) return;
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer),
                new MuzzleFlashPacket(pos, lightLevel));
    }
}
