package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.util.LaserToggleData;
import net.minecraft.world.item.ItemStack;

public final class ClientLaserToggleState {
    private static boolean toggleAllowed = true;
    private static ItemStack attachmentRenderGun = ItemStack.EMPTY;

    private ClientLaserToggleState() {}

    public static void setToggleAllowed(boolean allowed) {
        toggleAllowed = allowed;
    }

    public static boolean isToggleAllowed() {
        return toggleAllowed;
    }

    // A disabled or absent server feature forces lasers on regardless of saved per-gun state.
    public static boolean isLaserEnabled(ItemStack gunStack) {
        if (gunStack == null || gunStack.isEmpty()) return true;
        return !toggleAllowed || LaserToggleData.isEnabled(gunStack);
    }

    public static void beginAttachmentRender(ItemStack gunStack) {
        attachmentRenderGun = gunStack == null ? ItemStack.EMPTY : gunStack;
    }

    public static void endAttachmentRender() {
        attachmentRenderGun = ItemStack.EMPTY;
    }

    public static boolean isRenderedLaserEnabled(ItemStack renderedStack) {
        ItemStack safeRenderedStack = renderedStack == null ? ItemStack.EMPTY : renderedStack;
        return isLaserEnabled(attachmentRenderGun.isEmpty() ? safeRenderedStack : attachmentRenderGun);
    }

    public static void reset() {
        toggleAllowed = true;
        attachmentRenderGun = ItemStack.EMPTY;
    }
}
