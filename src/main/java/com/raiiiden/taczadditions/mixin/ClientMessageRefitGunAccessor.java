package com.raiiiden.taczadditions.mixin;

import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.network.message.ClientMessageRefitGun;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ClientMessageRefitGun.class, remap = false)
public interface ClientMessageRefitGunAccessor {
    @Accessor("gunSlotIndex")
    int taczadditions$getGunSlotIndex();

    @Accessor("attachmentType")
    AttachmentType taczadditions$getAttachmentType();
}
