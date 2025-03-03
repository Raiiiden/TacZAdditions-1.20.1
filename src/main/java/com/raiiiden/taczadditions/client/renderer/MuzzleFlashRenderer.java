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

        // Convert to block position with explicit casting
        BlockPos flashPos = new BlockPos(
                (int) Math.floor(flashPosVec.x),
                (int) Math.floor(flashPosVec.y),
                (int) Math.floor(flashPosVec.z)
        );

        level.setBlock(flashPos, Blocks.LIGHT.defaultBlockState(), 3);
        synchronized (flashPositions) {
            flashPositions.add(flashPos);
            flashTimes.add(System.currentTimeMillis());
        }

        System.out.println("[DEBUG] Muzzle flash triggered at: " + flashPos);

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
