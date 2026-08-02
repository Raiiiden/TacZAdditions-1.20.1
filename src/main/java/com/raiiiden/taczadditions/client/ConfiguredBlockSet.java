package com.raiiiden.taczadditions.client;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

// Resolves a configured list of block IDs and "#"-prefixed block tags once per config change.
// The laser trace consults these for every block it crosses, so re-parsing per frame is not an option.
public final class ConfiguredBlockSet {
    private final Supplier<List<? extends String>> source;

    private List<? extends String> cachedEntries;
    private Set<Block> blocks = Set.of();
    private Set<TagKey<Block>> blockTags = Set.of();

    public ConfiguredBlockSet(Supplier<List<? extends String>> source) {
        this.source = source;
    }

    public boolean matches(BlockState state) {
        refreshIfChanged();

        if (!blocks.isEmpty() && blocks.contains(state.getBlock())) return true;
        for (TagKey<Block> tag : blockTags) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    public void clear() {
        cachedEntries = null;
        blocks = Set.of();
        blockTags = Set.of();
    }

    private void refreshIfChanged() {
        List<? extends String> entries = source.get();
        // Config reloads and server sync both hand back a new list instance.
        if (entries == cachedEntries) return;
        cachedEntries = entries;

        Set<Block> resolved = new HashSet<>();
        Set<TagKey<Block>> tags = new HashSet<>();
        for (String entry : entries) {
            if (entry == null) continue;
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;

            boolean isTag = trimmed.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(isTag ? trimmed.substring(1) : trimmed);
            if (id == null) continue;

            if (isTag) {
                tags.add(TagKey.create(Registries.BLOCK, id));
                continue;
            }
            // Unknown IDs are simply absent; a pack may register them later, and clear() re-resolves.
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            if (block != null) resolved.add(block);
        }

        blocks = resolved;
        blockTags = tags;
    }
}
