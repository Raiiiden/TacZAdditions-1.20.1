package com.raiiiden.taczadditions.mixin;

import com.raiiiden.taczadditions.util.LaserToggleData;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.network.message.ClientMessageRefitGun;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientMessageRefitGun.class, remap = false)
public class ClientMessageRefitGunLaserToggleMixin {
    @Inject(
            method = "lambda$handle$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/api/item/IGun;installAttachment(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V",
                    shift = At.Shift.AFTER
            )
    )
    private static void turnOnInstalledLaser(NetworkEvent.Context context,
                                             ClientMessageRefitGun message,
                                             CallbackInfo ci) {
        ClientMessageRefitGunAccessor accessor = (ClientMessageRefitGunAccessor) (Object) message;
        if (accessor.taczadditions$getAttachmentType() != AttachmentType.LASER) return;

        ServerPlayer player = context.getSender();
        if (player == null) return;

        ItemStack gunStack = player.getInventory().getItem(accessor.taczadditions$getGunSlotIndex());
        if (gunStack.getItem() instanceof IGun
                && LaserToggleData.hasLaser(gunStack)) {
            LaserToggleData.setEnabled(gunStack, true);
        }
    }
}
