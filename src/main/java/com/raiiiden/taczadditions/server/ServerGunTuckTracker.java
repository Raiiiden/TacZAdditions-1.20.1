package com.raiiiden.taczadditions.server;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.raiiiden.taczadditions.util.GunTuckMath;
import com.tacz.guns.api.item.IGun;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

// Tracks authoritative tuck progress for each gun-holding entity.
@Mod.EventBusSubscriber
public final class ServerGunTuckTracker {
    private static final float TICK_SECONDS = 0.05f;
    private static final float IDLE_EPSILON = 1.0e-4f;

    // Server thread only; weak keys so entities that unload or die drop out on their own.
    private static final Map<LivingEntity, Float> PROGRESS = new WeakHashMap<>();

    private ServerGunTuckTracker() {
    }

    public static float getProgress(LivingEntity shooter) {
        if (shooter == null || shooter.level().isClientSide()) return 0f;
        Float progress = PROGRESS.get(shooter);
        return progress == null ? 0f : progress;
    }

    public static boolean isTuckEnforced() {
        return TacZAdditionsConfig.COMMON.tuckAffectsBulletAngle.get()
                || TacZAdditionsConfig.COMMON.blockFireWhenTucked.get();
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        if (!isTuckEnforced()) {
            if (!PROGRESS.isEmpty()) PROGRESS.clear();
            return;
        }

        if (!(entity.getMainHandItem().getItem() instanceof IGun)) {
            PROGRESS.remove(entity);
            return;
        }

        float distance = TacZAdditionsConfig.COMMON.tuckDistance.get().floatValue();
        float target = GunTuckMath.calculateTarget(entity, 1.0f, distance);
        Float current = PROGRESS.get(entity);
        float updated = GunTuckMath.advance(current == null ? 0f : current, target, TICK_SECONDS);

        if (updated <= IDLE_EPSILON) {
            PROGRESS.remove(entity);
        } else {
            PROGRESS.put(entity, updated);
        }
    }
}
