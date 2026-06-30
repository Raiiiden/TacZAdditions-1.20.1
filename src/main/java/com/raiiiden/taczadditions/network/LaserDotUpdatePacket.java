package com.raiiiden.taczadditions.network;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class LaserDotUpdatePacket {
    public final double x;
    public final double y;
    public final double z;
    public final int color;

    public LaserDotUpdatePacket(double x, double y, double z, int color) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
    }

    public static void encode(LaserDotUpdatePacket msg, FriendlyByteBuf buffer) {
        buffer.writeDouble(msg.x);
        buffer.writeDouble(msg.y);
        buffer.writeDouble(msg.z);
        buffer.writeInt(msg.color);
    }

    public static LaserDotUpdatePacket decode(FriendlyByteBuf buffer) {
        return new LaserDotUpdatePacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readInt()
        );
    }

    public static void handle(LaserDotUpdatePacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || !isValidLaserDot(sender, msg)) return;

            ModNetworking.relayLaserDot(sender, msg.x, msg.y, msg.z, msg.color);
        });
        context.setPacketHandled(true);
    }

    private static boolean isValidLaserDot(ServerPlayer sender, LaserDotUpdatePacket msg) {
        if (!Double.isFinite(msg.x) || !Double.isFinite(msg.y) || !Double.isFinite(msg.z)) {
            return false;
        }

        if (!isHoldingGunWithLaser(sender.getMainHandItem())) {
            return false;
        }

        double maxDistance = TacZAdditionsConfig.SERVER.laserDotMaxDistance.get() + 2.0;
        return sender.distanceToSqr(msg.x, msg.y, msg.z) <= maxDistance * maxDistance;
    }

    private static boolean isHoldingGunWithLaser(ItemStack gunStack) {
        if (gunStack.getItem() instanceof IGun gun) {
            ItemStack laserAttachment = gun.getAttachment(gunStack, AttachmentType.LASER);
            return !laserAttachment.isEmpty();
        }
        return false;
    }
}
