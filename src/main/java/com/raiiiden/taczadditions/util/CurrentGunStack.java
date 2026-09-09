package com.raiiiden.taczadditions.util;

import net.minecraft.world.item.ItemStack;

public class CurrentGunStack {

    private static final ThreadLocal<ItemStack> CURRENT_STACK = new ThreadLocal<>();

    public static void set(ItemStack stack) {
        CURRENT_STACK.set(stack);
    }

    public static ItemStack get() {
        return CURRENT_STACK.get();
    }

    public static void clear() {
        CURRENT_STACK.remove();
    }
}
