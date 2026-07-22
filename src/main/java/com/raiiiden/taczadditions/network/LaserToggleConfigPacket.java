package com.raiiiden.taczadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class LaserToggleConfigPacket {
    public final boolean toggleAllowed;

    public LaserToggleConfigPacket(boolean toggleAllowed) {
        this.toggleAllowed = toggleAllowed;
    }

    public static void encode(LaserToggleConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.toggleAllowed);
    }

    public static LaserToggleConfigPacket decode(FriendlyByteBuf buffer) {
        return new LaserToggleConfigPacket(buffer.readBoolean());
    }

    public static void handle(LaserToggleConfigPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> LaserToggleConfigPacketClientHandler.handle(packet));
        context.setPacketHandled(true);
    }
}
