package com.raiiiden.taczadditions.network;

import com.raiiiden.taczadditions.server.ServerFreeAimTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Reports where the sender's gun is pointing relative to their crosshair, in degrees.
public final class FreeAimUpdatePacket {

    private final float yawOffset;
    private final float pitchOffset;

    public FreeAimUpdatePacket(float yawOffset, float pitchOffset) {
        this.yawOffset = yawOffset;
        this.pitchOffset = pitchOffset;
    }

    public static void encode(FreeAimUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.yawOffset);
        buffer.writeFloat(packet.pitchOffset);
    }

    public static FreeAimUpdatePacket decode(FriendlyByteBuf buffer) {
        float yaw = buffer.readFloat();
        float pitch = buffer.readFloat();
        // A non-finite angle would poison every direction computed from it.
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) return new FreeAimUpdatePacket(0f, 0f);
        return new FreeAimUpdatePacket(yaw, pitch);
    }

    public static void handle(FreeAimUpdatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            ServerFreeAimTracker.report(player, packet.yawOffset, packet.pitchOffset);
        });
        context.setPacketHandled(true);
    }
}
