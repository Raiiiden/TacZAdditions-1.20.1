package com.raiiiden.taczadditions.registry;

import com.raiiiden.taczadditions.TaczAdditions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TaczAdditions.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TaczAdditions.MODID);

    // Example Item Registration
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item",
            () -> new Item(new Item.Properties()));

    // Example Armor Registration
    public static final RegistryObject<ArmorItem> EXAMPLE_HELMET = ITEMS.register("example_helmet",
            () -> new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<ArmorItem> EXAMPLE_CHESTPLATE = ITEMS.register("example_chestplate",
            () -> new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    // Creative Tab Registration
    public static final RegistryObject<CreativeModeTab> TACZADDITIONS_TAB = CREATIVE_MODE_TABS.register("taczadditions_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.taczadditions_tab"))
                    .icon(() -> new ItemStack(EXAMPLE_ITEM.get()))
                    .displayItems((enabledFeatures, entries) -> {
                        entries.accept(EXAMPLE_ITEM.get());
                        entries.accept(EXAMPLE_HELMET.get());
                        entries.accept(EXAMPLE_CHESTPLATE.get());
                    })
                    .build()
    );
}
