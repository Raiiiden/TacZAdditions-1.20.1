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
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MuzzleFlashRenderer {
    private static final List<BlockPos> flashPositions = new ArrayList<>(); // Stores all light block positions
    private static final List<Long> flashTimes = new ArrayList<>(); // Stores timestamps for each block
    private static final long FLASH_DURATION_MS = 10; // 10ms muzzle flash
    private static final long MAX_FLASH_LIFETIME_MS = 1000; // Safety removal after 1 second

    public static void triggerFlash() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Player player = mc.player;
        Level level = mc.level;

        // Get player's eye height
        double eyeHeight = player.getEyeY();

        // Get the player's forward direction
        double forwardX = -Math.sin(Math.toRadians(player.getYRot()));
        double forwardZ = Math.cos(Math.toRadians(player.getYRot()));

        // Calculate correct light block position (1 block ahead at head level)
        int x = (int) (player.getX() + forwardX);
        int y = (int) Math.floor(eyeHeight);
        int z = (int) (player.getZ() + forwardZ);

        BlockPos flashPos = new BlockPos(x, y, z);

        // Place the light block
        level.setBlock(flashPos, Blocks.LIGHT.defaultBlockState(), 3);
        flashPositions.add(flashPos);
        flashTimes.add(System.currentTimeMillis());

        System.out.println("[DEBUG] Muzzle flash triggered at: " + flashPos);

        // Register tick event for checking elapsed time
        if (!MinecraftForge.EVENT_BUS.isRegistered(MuzzleFlashRenderer.class)) {
            MinecraftForge.EVENT_BUS.register(new MuzzleFlashRenderer());
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (flashPositions.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        Iterator<BlockPos> posIterator = flashPositions.iterator();
        Iterator<Long> timeIterator = flashTimes.iterator();

        while (posIterator.hasNext() && timeIterator.hasNext()) {
            BlockPos flashPos = posIterator.next();
            long flashTime = timeIterator.next();

            if (currentTime - flashTime >= FLASH_DURATION_MS || currentTime - flashTime >= MAX_FLASH_LIFETIME_MS) {
                removeFlash(flashPos);
                posIterator.remove();
                timeIterator.remove();
            }
        }
    }

    private static void removeFlash(BlockPos flashPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockState(flashPos).getBlock() == Blocks.LIGHT) {
            mc.level.setBlock(flashPos, Blocks.AIR.defaultBlockState(), 3);
            System.out.println("[DEBUG] Light removed at: " + flashPos);
        }
    }
}