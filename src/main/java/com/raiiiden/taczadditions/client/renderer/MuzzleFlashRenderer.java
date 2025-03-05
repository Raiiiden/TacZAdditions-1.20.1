package com.raiiiden.taczadditions.client.renderer;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.phys.Vec3;
import it.unimi.dsi.fastutil.Pair;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class MuzzleFlashRenderer {
    private static final List<BlockPos> flashPositions = new ArrayList<>();
    private static final List<Long> flashTimes = new ArrayList<>();
    private static final long FLASH_DURATION_MS = 10;
    private static final long MAX_FLASH_LIFETIME_MS = 1000;

    private static boolean isRegistered = false;

    public static void triggerFlash(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || player == null) return;

        Level level = mc.level;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle().normalize();
        Vec3 flashPosVec = eyePos.add(lookVec.scale(1.0));
        BlockPos flashPos = new BlockPos((int) Math.floor(flashPosVec.x), (int) Math.floor(flashPosVec.y), (int) Math.floor(flashPosVec.z));

        int lightLevel = isGunSilenced(player) ? 6 : 15;

        if (level.getBlockState(flashPos).isAir()) {
            IntegerProperty lightProperty = (IntegerProperty) Blocks.LIGHT.getStateDefinition().getProperties().iterator().next();
            level.setBlock(flashPos, Blocks.LIGHT.defaultBlockState().setValue(lightProperty, lightLevel), 3);

            synchronized (flashPositions) {
                flashPositions.add(flashPos);
                flashTimes.add(System.currentTimeMillis());
            }

            System.out.println("[DEBUG] Muzzle flash triggered at: " + flashPos + " with light level: " + lightLevel);
        }

        if (!isRegistered) {
            MinecraftForge.EVENT_BUS.register(new MuzzleFlashRenderer());
            isRegistered = true;
        }
    }

    private static boolean isGunSilenced(Player player) {
        if (player == null) return false;

        IGunOperator operator = IGunOperator.fromLivingEntity(player);
        if (operator == null) return false;

        ShooterDataHolder dataHolder = operator.getDataHolder();
        if (dataHolder == null) return false;

        ItemStack gunItem = dataHolder.currentGunItem != null ? dataHolder.currentGunItem.get() : ItemStack.EMPTY;
        if (gunItem.isEmpty()) return false;

        AttachmentCacheProperty cacheProperty = operator.getCacheProperty();
        if (cacheProperty == null) return false;

        Object silenceData = cacheProperty.getCache(SilenceModifier.ID);

        if (silenceData instanceof Pair<?, ?> pair) {
            Object rightValue = pair.right();
            if (rightValue instanceof Boolean booleanValue) {
                return booleanValue;
            }
        }
        return false;
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
