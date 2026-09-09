package com.raiiiden.taczadditions.server;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

// Holds the free aim offset each client last reported, so a shot can leave along the barrel the
// player is actually looking at rather than along the crosshair.
public final class ServerFreeAimTracker {

    // Unlike gun tuck the server cannot work this out for itself: it follows from the player's own
    // mouse and client tuning, so it is reported, clamped here, and dropped when it goes stale.
    private static final long MAX_AGE_TICKS = 5L;

    private record Offset(float yaw, float pitch, long tick) {
    }

    // Server thread only; weak keys so players who disconnect drop out on their own.
    private static final Map<LivingEntity, Offset> OFFSETS = new WeakHashMap<>();

    private ServerFreeAimTracker() {
    }

    public static boolean isEnforced() {
        return TacZAdditionsConfig.COMMON.enableFreeAim.get()
                && TacZAdditionsConfig.COMMON.freeAimAffectsBulletAngle.get();
    }

    // The reported angles are clamped to the server's own limit, never trusted as sent.
    public static void report(ServerPlayer player, float yawOffset, float pitchOffset) {
        if (player == null) return;
        if (!isEnforced()) {
            OFFSETS.remove(player);
            return;
        }

        float limit = TacZAdditionsConfig.COMMON.freeAimMaxBulletAngle.get().floatValue();
        float yaw = Mth.clamp(yawOffset, -limit, limit);
        float pitch = Mth.clamp(pitchOffset, -limit, limit);

        if (yaw == 0f && pitch == 0f) {
            OFFSETS.remove(player);
            return;
        }
        OFFSETS.put(player, new Offset(yaw, pitch, player.level().getGameTime()));
    }

    public static float yawOffset(LivingEntity shooter) {
        Offset offset = current(shooter);
        return offset == null ? 0f : offset.yaw();
    }

    public static float pitchOffset(LivingEntity shooter) {
        Offset offset = current(shooter);
        return offset == null ? 0f : offset.pitch();
    }

    private static Offset current(LivingEntity shooter) {
        if (shooter == null || shooter.level().isClientSide()) return null;
        if (!isEnforced()) return null;

        Offset offset = OFFSETS.get(shooter);
        if (offset == null) return null;
        // A client that stops reporting stops bending its shots within a few ticks.
        if (shooter.level().getGameTime() - offset.tick() > MAX_AGE_TICKS) return null;
        return offset;
    }
}
