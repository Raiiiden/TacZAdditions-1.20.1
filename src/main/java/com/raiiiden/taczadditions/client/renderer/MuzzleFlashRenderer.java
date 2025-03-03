package com.raiiiden.taczadditions.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MuzzleFlashRenderer {
    private static final List<BlockPos> flashPositions = new ArrayList<>();
    private static final List<Long> flashTimes = new ArrayList<>();
    private static final long FLASH_DURATION_MS = 10; // 10ms muzzle flash
    private static final long MAX_FLASH_LIFETIME_MS = 1000; // Safety removal after 1 second

    private static boolean isRegistered = false;

    public static void triggerFlash() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Player player = mc.player;
        Level level = mc.level;

        Vec3 eyePos = player.getEyePosition(); // Player's head position
        Vec3 lookVec = player.getLookAngle().normalize(); // Direction player is looking

        // Offset forward by 1 block
        double distance = 1.0;
        Vec3 flashPosVec = eyePos.add(lookVec.scale(distance));

        // Convert to block position
        BlockPos initialPos = new BlockPos(
                (int) Math.floor(flashPosVec.x),
                (int) Math.floor(flashPosVec.y),
                (int) Math.floor(flashPosVec.z)
        );

        // Find a valid air block within 3x3 area
        BlockPos validFlashPos = findNearestAirBlock(level, initialPos);
        if (validFlashPos == null) {
            System.out.println("[DEBUG] No valid air block found for muzzle flash.");
            return;
        }

        level.setBlock(validFlashPos, Blocks.LIGHT.defaultBlockState(), 3);
        synchronized (flashPositions) {
            flashPositions.add(validFlashPos);
            flashTimes.add(System.currentTimeMillis());
        }

        System.out.println("[DEBUG] Muzzle flash triggered at: " + validFlashPos);

        if (!isRegistered) {
            MinecraftForge.EVENT_BUS.register(new MuzzleFlashRenderer());
            isRegistered = true;
        }
    }

    private static BlockPos findNearestAirBlock(Level level, BlockPos centerPos) {
        // Check if the intended position is already air
        if (level.getBlockState(centerPos).isAir()) {
            return centerPos;
        }

        // Search for the closest air block in a 3x3 area
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos checkPos = centerPos.offset(dx, dy, dz);
                    if (level.getBlockState(checkPos).isAir()) {
                        return checkPos; // Return the first valid air block found
                    }
                }
            }
        }
        return null; // No air block found within 3x3 area
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
