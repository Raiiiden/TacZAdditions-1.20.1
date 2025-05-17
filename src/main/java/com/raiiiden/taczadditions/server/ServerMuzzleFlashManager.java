package com.raiiiden.taczadditions.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber
public class ServerMuzzleFlashManager {
    private static final Map<ServerLevel, Map<BlockPos, Long>> activeFlashes = new HashMap<>();
    private static final long FLASH_DURATION_MS = 10;

    public static void placeFlash(ServerLevel level, BlockPos pos, int lightLevel) {
        BlockPos targetPos = findValidPosition(level, pos);
        if (targetPos == null) return;

        try {
            IntegerProperty lightProp = (IntegerProperty) Blocks.LIGHT.getStateDefinition().getProperty("level");
            level.setBlock(targetPos, Blocks.LIGHT.defaultBlockState().setValue(lightProp, lightLevel), 3);
            activeFlashes.computeIfAbsent(level, l -> new HashMap<>()).put(targetPos, System.currentTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static BlockPos findValidPosition(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).isAir()) return pos;
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos nearby = pos.offset(dx, dy, dz);
                    if (level.getBlockState(nearby).isAir()) return nearby;
                }
        return null;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<ServerLevel, Map<BlockPos, Long>>> it = activeFlashes.entrySet().iterator();
        while (it.hasNext()) {
            var levelEntry = it.next();
            var map = levelEntry.getValue();
            var level = levelEntry.getKey();

            map.entrySet().removeIf(entry -> {
                if (now - entry.getValue() >= FLASH_DURATION_MS) {
                    if (level.getBlockState(entry.getKey()).getBlock() == Blocks.LIGHT)
                        level.setBlock(entry.getKey(), Blocks.AIR.defaultBlockState(), 3);
                    return true;
                }
                return false;
            });

            if (map.isEmpty()) it.remove();
        }
    }
}
