package com.raiiiden.taczadditions.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class MuzzleFlashRenderer {
    private static final List<BlockPos> flashPositions = new ArrayList<>();
    private static final List<Long> flashTimes = new ArrayList<>();
    private static final long FLASH_DURATION_MS = 10;
    private static final long MAX_FLASH_LIFETIME_MS = 1000;

    public static void triggerFlash() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Player player = mc.player;
        Level level = mc.level;

        // Use raycast to find block where player is aiming
        BlockPos flashPos = raycastFromPlayer(player, 2.0);

        level.setBlock(flashPos, Blocks.LIGHT.defaultBlockState(), 3);
        flashPositions.add(flashPos);
        flashTimes.add(System.currentTimeMillis());

        System.out.println("[DEBUG] Muzzle flash triggered at: " + flashPos);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (flashPositions.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        Iterator<BlockPos> posIterator = flashPositions.iterator();
        Iterator<Long> timeIterator = flashTimes.iterator();

        while (posIterator.hasNext() && timeIterator.hasNext()) {
            BlockPos flashPos = posIterator.next();
            long flashTime = timeIterator.next();

            if (currentTime - flashTime >= FLASH_DURATION_MS) {
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

    private static BlockPos raycastFromPlayer(Player player, double maxDistance) {
        double yaw = Math.toRadians(player.getYRot());
        double pitch = Math.toRadians(player.getXRot());

        double dx = -Math.sin(yaw) * Math.cos(pitch);
        double dy = -Math.sin(pitch);
        double dz = Math.cos(yaw) * Math.cos(pitch);

        double x = player.getX() + dx * maxDistance;
        double y = player.getEyeY() + dy * maxDistance;
        double z = player.getZ() + dz * maxDistance;

        return new BlockPos((int) x, (int) y, (int) z);
    }
}
