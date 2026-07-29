package com.raiiiden.taczadditions.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1.3";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("taczadditions", "network"),
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
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
        CHANNEL.registerMessage(
                id++,
                LaserDotUpdatePacket.class,
                LaserDotUpdatePacket::encode,
                LaserDotUpdatePacket::decode,
                LaserDotUpdatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                id++,
                LaserDotSyncPacket.class,
                LaserDotSyncPacket::encode,
                LaserDotSyncPacket::decode,
                LaserDotSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                LaserToggleRequestPacket.class,
                LaserToggleRequestPacket::encode,
                LaserToggleRequestPacket::decode,
                LaserToggleRequestPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                id++,
                ConfigSnapshotPacket.class,
                ConfigSnapshotPacket::encode,
                ConfigSnapshotPacket::decode,
                ConfigSnapshotPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                LaserToggleConfigPacket.class,
                LaserToggleConfigPacket::encode,
                LaserToggleConfigPacket::decode,
                LaserToggleConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    // Dynamic lights fade out well within this range, so viewers further away gain nothing.
    private static final double MUZZLE_FLASH_VIEW_RANGE_SQR = 128.0 * 128.0;

    // Sends nearby living-shooter flashes only through available client channels.
    public static void sendMuzzleFlash(LivingEntity shooter, int lightLevel, int color) {
        if (!(shooter.level() instanceof ServerLevel serverLevel)) return;

        MuzzleFlashPacket packet = new MuzzleFlashPacket(shooter.getId(), lightLevel, color);
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(shooter) > MUZZLE_FLASH_VIEW_RANGE_SQR) continue;
            sendToPlayerIfPresent(player, packet);
        }
    }

    public static void sendLaserDot(Vec3 hitPos, int color) {
        CHANNEL.sendToServer(new LaserDotUpdatePacket(hitPos.x, hitPos.y, hitPos.z, color));
    }

    public static void sendLaserToggleRequest() {
        CHANNEL.sendToServer(new LaserToggleRequestPacket());
    }

    // Locks the joining player's gun handling to this server's common config for the session.
    public static void sendConfigSnapshot(ServerPlayer player) {
        sendToPlayerIfPresent(player, ConfigSnapshotPacket.ofLocalValues());
    }

    public static void sendLaserToggleConfig(ServerPlayer player) {
        sendToPlayerIfPresent(player,
                new LaserToggleConfigPacket(TacZAdditionsConfig.COMMON.enableLaserToggle.get()));
    }

    public static void relayLaserDot(ServerPlayer sender, double x, double y, double z, int color) {
        if (sender.getServer() == null) return;

        LaserDotSyncPacket packet = new LaserDotSyncPacket(sender.getId(), x, y, z, color);
        for (ServerPlayer player : sender.getServer().getPlayerList().getPlayers()) {
            if (player != sender && player.level() == sender.level()) {
                sendToPlayerIfPresent(player, packet);
            }
        }
    }

    private static void sendToPlayerIfPresent(ServerPlayer player, Object packet) {
        if (player.connection == null || !CHANNEL.isRemotePresent(player.connection.connection)) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
