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

@OnlyIn(Dist.CLIENT)
public class MuzzleFlashRenderer {
    private static BlockPos lastFlashPos = null;
    private static long flashStartTime = 0;
    private static final long FLASH_DURATION_MS = 10; // 10ms muzzle flash

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
        lastFlashPos = flashPos;
        flashStartTime = System.currentTimeMillis();

        System.out.println("[DEBUG] Muzzle flash triggered at: " + flashPos);

        // Register tick event for checking elapsed time
        MinecraftForge.EVENT_BUS.register(MuzzleFlashRenderer.class);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (lastFlashPos != null && System.currentTimeMillis() - flashStartTime >= FLASH_DURATION_MS) {
            removeFlash();
            MinecraftForge.EVENT_BUS.unregister(MuzzleFlashRenderer.class); // Stop checking once removed
        }
    }

    private static void removeFlash() {
        Minecraft mc = Minecraft.getInstance();
        if (lastFlashPos != null && mc.level != null) {
            mc.level.setBlock(lastFlashPos, Blocks.AIR.defaultBlockState(), 3);
            System.out.println("[DEBUG] Light removed at: " + lastFlashPos);
            lastFlashPos = null;
        }
    }
}
