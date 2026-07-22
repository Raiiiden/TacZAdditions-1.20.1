package com.raiiiden.taczadditions.network;

import net.minecraft.resources.ResourceLocation;
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
    private static final String PROTOCOL_VERSION = "1.1";

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
                LaserToggleConfigPacket.class,
                LaserToggleConfigPacket::encode,
                LaserToggleConfigPacket::decode,
                LaserToggleConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    // Sends muzzle-flash packets only to clients that expose this optional channel.
    public static void sendMuzzleFlash(LivingEntity shooter, int lightLevel) {
        if (!(shooter instanceof ServerPlayer serverPlayer)) return;
        if (serverPlayer.getServer() == null) return;

        MuzzleFlashPacket packet = new MuzzleFlashPacket(shooter.getId(), lightLevel);
        for (ServerPlayer player : serverPlayer.getServer().getPlayerList().getPlayers()) {
            if (player.level() == shooter.level()) sendToPlayerIfPresent(player, packet);
        }
    }

    public static void sendLaserDot(Vec3 hitPos, int color) {
        CHANNEL.sendToServer(new LaserDotUpdatePacket(hitPos.x, hitPos.y, hitPos.z, color));
    }

    public static void sendLaserToggleRequest() {
        CHANNEL.sendToServer(new LaserToggleRequestPacket());
    }

    public static void sendLaserToggleConfig(ServerPlayer player) {
        sendToPlayerIfPresent(player,
                new LaserToggleConfigPacket(TacZAdditionsConfig.SERVER.enableLaserToggle.get()));
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
