package com.raiiiden.taczadditions.util;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class LaserToggleData {
    private static final String ENABLED_TAG = "taczadditions:LaserEnabled";

    private LaserToggleData() {}

    public static boolean hasLaser(ItemStack gunStack) {
        if (gunStack == null || gunStack.isEmpty()) return false;
        if (!(gunStack.getItem() instanceof IGun gun)) return false;

        ItemStack attachment = gun.getAttachment(gunStack, AttachmentType.LASER);
        if (!attachment.isEmpty()) return true;

        return !gun.getBuiltinAttachment(gunStack, AttachmentType.LASER).isEmpty();
    }

    public static boolean isEnabled(ItemStack gunStack) {
        if (gunStack == null || gunStack.isEmpty()) return true;
        return !gunStack.hasTag()
                || !gunStack.getTag().contains(ENABLED_TAG, Tag.TAG_BYTE)
                || gunStack.getTag().getBoolean(ENABLED_TAG);
    }

    public static void setEnabled(ItemStack gunStack, boolean enabled) {
        if (gunStack == null || gunStack.isEmpty()) return;
        if (enabled) {
            if (gunStack.hasTag()) gunStack.getTag().remove(ENABLED_TAG);
        } else {
            gunStack.getOrCreateTag().putBoolean(ENABLED_TAG, false);
        }
    }
}
