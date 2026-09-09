package com.raiiiden.taczadditions.network;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.raiiiden.taczadditions.registry.ModSounds;
import com.raiiiden.taczadditions.util.LaserToggleData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class LaserToggleRequestPacket {
    public static void encode(LaserToggleRequestPacket packet, FriendlyByteBuf buffer) {}

    public static LaserToggleRequestPacket decode(FriendlyByteBuf buffer) {
        return new LaserToggleRequestPacket();
    }

    public static void handle(LaserToggleRequestPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ModNetworking.sendLaserToggleConfig(player);
            if (!TacZAdditionsConfig.COMMON.enableLaserToggle.get()) return;

            ItemStack gunStack = player.getMainHandItem();
            if (!LaserToggleData.hasLaser(gunStack)) return;

            boolean enabled = !LaserToggleData.isEnabled(gunStack);
            LaserToggleData.setEnabled(gunStack, enabled);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            player.level().playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    ModSounds.LASER_CLICK.get(), SoundSource.PLAYERS,
                    0.7f, 1.0f
            );
        });
        context.setPacketHandled(true);
    }
}
