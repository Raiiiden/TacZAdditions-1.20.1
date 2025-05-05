package com.raiiiden.taczadditions.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MuzzleFlashRenderer {
    private static final List<BlockPos> flashPositions = new ArrayList<>();
    private static final List<Long> flashTimes = new ArrayList<>();
    private static final long FLASH_DURATION_MS = 10;
    private static final long MAX_FLASH_LIFETIME_MS = 1000;

    private static boolean isRegistered = false;

    public static void triggerFlashAt(BlockPos flashPos, int lightLevel) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            System.out.println("[DEBUG] Level is null.");
            return;
        }

        try {
            BlockPos targetPos = null;

            // Check if the initial position is air
            if (level.getBlockState(flashPos).isAir()) {
                targetPos = flashPos;
            } else {
                // Search for the nearest air block in a 3x3 area
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            BlockPos nearbyPos = flashPos.offset(dx, dy, dz);
                            if (level.getBlockState(nearbyPos).isAir()) {
                                targetPos = nearbyPos;
                                break;
                            }
                        }
                        if (targetPos != null) break;
                    }
                    if (targetPos != null) break;
                }
            }

            // If no air block is found, do nothing
            if (targetPos == null) {
                System.out.println("[DEBUG] No suitable air block found near: " + flashPos);
                return;
            }

            IntegerProperty lightProperty = (IntegerProperty) Blocks.LIGHT.getStateDefinition().getProperty("level");
            level.setBlock(targetPos, Blocks.LIGHT.defaultBlockState().setValue(lightProperty, lightLevel), 3);

            synchronized (flashPositions) {
                flashPositions.add(targetPos);
                flashTimes.add(System.currentTimeMillis());
            }

            System.out.println("[DEBUG] Muzzle flash placed at: " + targetPos + " with light level: " + lightLevel);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!isRegistered) {
            MinecraftForge.EVENT_BUS.register(new MuzzleFlashRenderer());
            isRegistered = true;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (flashPositions.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        List<BlockPos> toRemove = new ArrayList<>();

        synchronized (flashPositions) {
            Iterator<BlockPos> posIterator = flashPositions.iterator();
            Iterator<Long> timeIterator = flashTimes.iterator();

            while (posIterator.hasNext() && timeIterator.hasNext()) {
                BlockPos flashPos = posIterator.next();
                long flashTime = timeIterator.next();

                if (currentTime - flashTime >= FLASH_DURATION_MS || currentTime - flashTime >= MAX_FLASH_LIFETIME_MS) {
                    toRemove.add(flashPos);
                    timeIterator.remove();
                }
            }
        }

        for (BlockPos pos : toRemove) {
            removeFlash(pos);
        }

        if (flashPositions.isEmpty()) {
            MinecraftForge.EVENT_BUS.unregister(this);
            isRegistered = false;
        }
    }

    private static void removeFlash(BlockPos flashPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockState(flashPos).getBlock() == Blocks.LIGHT) {
            mc.level.setBlock(flashPos, Blocks.AIR.defaultBlockState(), 3);
            System.out.println("[DEBUG] Light removed at: " + flashPos);
        }
        synchronized (flashPositions) {
            flashPositions.remove(flashPos);
        }
    }
}
