package com.raiiiden.taczadditions.server;

import com.raiiiden.taczadditions.TaczAdditions;
import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.raiiiden.taczadditions.registry.ModParticles;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Bubble trails behind bullets travelling through water, and a splash where they cross the surface.
 *
 * <p>Server driven: the particles are spawned from the authoritative bullet and sent to every player
 * in range, so everyone in a firefight sees the same water. The splash is a ring of {@code water_puff}
 * thrown in three staggered waves, with a column of {@code water_drop} out of the middle that arcs
 * over and falls back out. The trail is {@code water_bubble}. All three draw vanilla particle art.</p>
 *
 * <p>Run from the bullet's own tick rather than a sweep of live entities, because a bullet that hits
 * something is discarded during its tick and would be gone before any post-tick pass ran.</p>
 */
@Mod.EventBusSubscriber(modid = TaczAdditions.MODID)
public final class BulletWaterEffects {

    // How finely the flight path is walked when looking for the surface. Fine enough to catch a
    // one-block puddle at full bullet speed without turning into a per-block query storm.
    private static final double SAMPLE_STEP = 0.25D;
    private static final int MAX_SAMPLES = 160;
    // Bubble spacing at density 1.0, in blocks.
    private static final double BASE_TRAIL_SPACING = 0.3D;
    // Below this much travel in a tick the bullet has effectively stopped and the trail ends with it.
    private static final double TRAIL_MIN_TRAVEL = 0.03D;
    // How far the first bubble sits past the surface. Without it the entry bubbles straddle the water
    // plane and clip through it; a fraction of a block in, they read as being under the water.
    private static final double TRAIL_ENTRY_INSET = 0.22D;
    private static final int MAX_CROSSINGS_PER_BULLET = 4;
    private static final int MAX_SOUNDS_PER_TICK = 3;
    private static final int MAX_PENDING_WAVES = 256;

    private static int budgetRemaining;
    private static int soundsThisTick;
    private static final List<PendingWave> PENDING_WAVES = new ArrayList<>();

    // A later wave of the same splash. Staggering them is what makes the ring read as spreading
    // outward rather than as one puff that appears and fades.
    private record PendingWave(ServerLevel level, Vec3 at, Vec3 lean, double intensity, long fireTime,
                               int puffs, double elevation, double speedScale, double startRadius) {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            budgetRemaining = TacZAdditionsConfig.COMMON.bulletWaterParticleBudget.get();
            soundsThisTick = 0;
            return;
        }

        if (PENDING_WAVES.isEmpty()) return;
        for (Iterator<PendingWave> it = PENDING_WAVES.iterator(); it.hasNext(); ) {
            PendingWave wave = it.next();
            if (wave.level().getGameTime() < wave.fireTime()) continue;
            it.remove();
            spawnRing(wave.level(), wave.level().getRandom(), wave.at(), wave.lean(), wave.intensity(),
                    wave.puffs(), wave.elevation(), wave.speedScale(), wave.startRadius());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PENDING_WAVES.clear();
    }

    /** Called from the server side of every TaCZ bullet tick, discarded bullets included. */
    public static void onBulletTicked(EntityKineticBullet bullet) {
        if (!TacZAdditionsConfig.COMMON.bulletWaterEffects.get()) return;
        if (budgetRemaining <= 0) return;
        if (!(bullet.level() instanceof ServerLevel level)) return;

        Vec3 start = new Vec3(bullet.xOld, bullet.yOld, bullet.zOld);
        Vec3 end = bullet.position();

        // How fast the round is actually going, measured before the segment is clipped. Clipping only
        // says where the particles may be drawn; using its length as the speed made a splash shrink
        // just because the bullet struck something soon after entering the water, which is why a hit
        // close to the muzzle threw a smaller splash than the same shot taken from further off.
        double travelled = start.distanceTo(end);

        // TaCZ moves the bullet the full tick even when it hit something on the way, so the raw
        // segment can run through walls. Clipping keeps the trail and the splash on this side of it.
        BlockHitResult hit = level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bullet));
        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 stopped = hit.getLocation();
            Vec3 back = start.subtract(end);
            double backLength = back.length();
            end = backLength > 1.0E-4D ? stopped.add(back.scale(0.002D / backLength)) : stopped;
        }

        traceSegment(level, bullet.getRandom(), start, end, travelled, weaponScale(bullet));
    }

    private static void traceSegment(ServerLevel level, RandomSource random, Vec3 start, Vec3 end,
                                     double travelled, double weaponScale) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0E-4D) return;

        int samples = Mth.clamp((int) Math.ceil(length / SAMPLE_STEP), 2, MAX_SAMPLES);
        double spacing = BASE_TRAIL_SPACING / TacZAdditionsConfig.COMMON.bulletWaterTrailDensity.get();
        Vec3 direction = delta.scale(1.0D / length);

        boolean previousWet = isUnderWater(level, start.x, start.y, start.z);
        Vec3 previousPoint = start;
        // Start of the stretch the bullet is currently travelling through water, if it is in one, and
        // whether that stretch began at the surface this tick rather than carrying on from last tick.
        Vec3 runStart = previousWet ? start : null;
        boolean runFromSurface = false;
        int crossings = 0;

        for (int i = 1; i <= samples; i++) {
            Vec3 point = start.add(delta.scale((double) i / samples));
            boolean wet = isUnderWater(level, point.x, point.y, point.z);

            if (wet != previousWet) {
                Vec3 boundary = refineCrossing(level, previousPoint, point, previousWet);

                if (wet) {
                    runStart = boundary;
                    runFromSurface = isWaterSurface(level, boundary);
                } else {
                    if (runStart != null) {
                        emitTrail(level, random, runStart, boundary, direction, spacing, travelled, runFromSurface);
                    }
                    runStart = null;
                    runFromSurface = false;
                }

                // Only a real water surface throws a splash. Every other water boundary is the bullet
                // burying itself in something: a submerged wall, the lake bed, a waterlogged block.
                if (crossings < MAX_CROSSINGS_PER_BULLET && isWaterSurface(level, boundary)) {
                    crossings++;
                    spawnSurfaceSplash(level, random, boundary, direction, travelled, wet, weaponScale);
                }
            }

            previousWet = wet;
            previousPoint = point;
        }

        // Still in water when the tick ended: trail the rest of the way, so the bubbles keep following
        // the round for as long as it is moving instead of stopping at the entry point.
        if (runStart != null) emitTrail(level, random, runStart, end, direction, spacing, travelled, runFromSurface);
    }

    // Minecraft water is a stack of horizontal planes, so a genuine surface has water just below the
    // crossing and open air just above it. A submerged wall or the lake bed has water above instead,
    // which is what tells the two apart no matter how the block shapes line up.
    private static boolean isWaterSurface(ServerLevel level, Vec3 point) {
        if (!isUnderWater(level, point.x, point.y - 0.06D, point.z)) return false;
        if (isUnderWater(level, point.x, point.y + 0.06D, point.z)) return false;
        return isOpenSpace(level, point.x, point.y + 0.06D, point.z);
    }

    // True when the point sits below the actual fluid height of its block, so a flowing or partially
    // filled block still reports its real surface instead of the whole block counting as water.
    private static boolean isUnderWater(ServerLevel level, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        FluidState fluid = level.getFluidState(pos);
        if (fluid.isEmpty() || !fluid.is(FluidTags.WATER)) return false;
        return y <= pos.getY() + fluid.getHeight(level, pos);
    }

    // True when the point is in air or in something a bullet passes through, rather than inside a block.
    private static boolean isOpenSpace(ServerLevel level, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.getCollisionShape(level, pos).isEmpty();
    }

    // Narrows the sampled interval down to the actual boundary, so the splash lands on the water
    // plane rather than up to a quarter block off it.
    private static Vec3 refineCrossing(ServerLevel level, Vec3 outside, Vec3 inside, boolean outsideWet) {
        Vec3 low = outside;
        Vec3 high = inside;
        for (int i = 0; i < 8; i++) {
            Vec3 mid = low.add(high).scale(0.5D);
            if (isUnderWater(level, mid.x, mid.y, mid.z) == outsideWet) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return low.add(high).scale(0.5D);
    }

    // Lays bubbles evenly along one underwater stretch of this tick's path.
    private static void emitTrail(ServerLevel level, RandomSource random, Vec3 from, Vec3 to,
                                  Vec3 direction, double spacing, double speed, boolean fromSurface) {
        double length = from.distanceTo(to);
        if (fromSurface) {
            // Hold the first bubble back a little so it starts under the water rather than in the
            // surface itself. Only where the stretch begins at the surface: a run carried over from
            // last tick is already well under.
            double inset = Math.min(TRAIL_ENTRY_INSET, length * 0.5D);
            from = from.add(direction.scale(inset));
            length -= inset;
        }
        if (length < TRAIL_MIN_TRAVEL) return;

        int count = Math.max(1, (int) Math.round(length / spacing));
        Vec3 step = to.subtract(from).scale(1.0D / count);
        double drag = Math.min(0.09D, speed * 0.004D);

        for (int i = 0; i < count; i++) {
            if (!spend(1)) return;
            Vec3 point = from.add(step.scale(i + random.nextDouble()));
            double jitter = 0.03D + random.nextDouble() * 0.05D;
            double px = point.x + (random.nextDouble() - 0.5D) * jitter;
            double py = point.y + (random.nextDouble() - 0.5D) * jitter;
            double pz = point.z + (random.nextDouble() - 0.5D) * jitter;
            // The jitter can lift a bubble out through the surface, where it pops on its first tick
            // and reads as clipping through the water.
            if (!isUnderWater(level, px, py, pz)) continue;

            sendParticle(level, ModParticles.WATER_BUBBLE.get(), px, py, pz,
                    0, direction.x, direction.y * 0.5D, direction.z,
                    drag * (0.4D + random.nextDouble() * 0.6D));
        }
    }

    private static void spawnSurfaceSplash(ServerLevel level, RandomSource random, Vec3 surface,
                                           Vec3 direction, double speed, boolean entering,
                                           double weaponScale) {
        double scale = TacZAdditionsConfig.COMMON.bulletWaterSplashScale.get() * weaponScale;
        // Bullet speeds vary by an order of magnitude between pistols and rifles; clamped so a slow
        // round still throws a visible splash and a magnum does not erupt.
        double intensity = Mth.clamp(speed / 10.0D, 0.6D, 1.9D) * scale;
        // How far the splash throws itself. Held at half the intensity so the particle counts stay
        // dense while the splash itself covers half the ground it used to.
        double reach = intensity * 0.5D;

        // A grazing shot leans its splash along the shot, a steep one throws it straight up.
        double steepness = Math.abs(direction.y);
        Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
        double flatLength = flat.length();
        flat = flatLength > 1.0E-4D ? flat.scale(1.0D / flatLength) : Vec3.ZERO;
        Vec3 lean = flat.scale(0.35D * reach * (1.0D - steepness));

        double x = surface.x;
        double y = surface.y;
        double z = surface.z;

        // Wave one: the ring at the impact, thrown up and out off the surface.
        int ring = 10 + (int) (16.0D * intensity);
        spawnRing(level, random, surface, lean, reach, ring, 0.5D, 1.0D, 0.04D);

        // Two more waves, each starting where the last one reached, flatter and faster, so the ring
        // visibly travels outward across the surface instead of appearing all at once. This is the
        // shape Superb Warfare gets from its three delayed cloud rings, sized for a bullet.
        long now = level.getGameTime();
        queueWave(new PendingWave(level, surface, lean, reach, now + 2L,
                Math.max(6, (int) (ring * 0.7D)), 0.26D, 1.3D, 0.18D * reach));
        queueWave(new PendingWave(level, surface, lean, reach, now + 5L,
                Math.max(4, (int) (ring * 0.45D)), 0.1D, 1.55D, 0.38D * reach));

        // The column of water standing up out of the hole, which is what gives the splash its height.
        int column = 4 + (int) (5.0D * intensity);
        for (int i = 0; i < column && spend(1); i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double drift = random.nextDouble() * 0.12D;
            Vec3 velocity = new Vec3(Math.cos(angle) * drift, 1.0D, Math.sin(angle) * drift)
                    .normalize()
                    .add(lean.scale(0.3D));
            double magnitude = (0.13D + random.nextDouble() * 0.11D) * reach * (0.6D + 0.4D * steepness);

            sendParticle(level, ModParticles.WATER_PUFF.get(),
                    x + Math.cos(angle) * 0.03D, y + 0.03D + random.nextDouble() * 0.05D,
                    z + Math.sin(angle) * 0.03D,
                    0, velocity.x, velocity.y, velocity.z, magnitude);
        }

        // Droplets thrown out of the middle: up hard, arcing over and raining back down around the
        // hole. Gravity shapes these, so they keep their speed rather than damping like the spray.
        int drops = 6 + (int) (9.0D * intensity);
        for (int i = 0; i < drops && spend(1); i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double spread = (0.04D + random.nextDouble() * 0.14D) * reach;
            double climb = (0.24D + random.nextDouble() * 0.26D) * reach * (0.65D + 0.35D * steepness);

            Vec3 velocity = new Vec3(Math.cos(angle) * spread, climb, Math.sin(angle) * spread).add(lean.scale(0.5D));
            double magnitude = velocity.length();
            if (magnitude < 1.0E-4D) continue;

            sendParticle(level, ModParticles.WATER_DROP.get(),
                    x + Math.cos(angle) * 0.04D, y + 0.03D, z + Math.sin(angle) * 0.04D,
                    0, velocity.x / magnitude, velocity.y / magnitude, velocity.z / magnitude, magnitude);
        }

        playSplashSound(level, x, y, z, intensity);
    }

    // One ring of spray. Elevation runs from 0.85 (steep crown) down to 0.2 (a flat sheet skidding
    // across the surface), which is what separates the three waves.
    private static void spawnRing(ServerLevel level, RandomSource random, Vec3 at, Vec3 lean,
                                  double intensity, int puffs, double elevation, double speedScale,
                                  double startRadius) {
        double angleOffset = random.nextDouble() * Mth.TWO_PI;
        for (int i = 0; i < puffs && spend(1); i++) {
            double angle = angleOffset + (Mth.TWO_PI * i) / puffs + (random.nextDouble() - 0.5D) * 0.5D;
            double climb = elevation * (0.7D + random.nextDouble() * 0.6D);
            double outward = 1.0D;

            Vec3 velocity = new Vec3(Math.cos(angle) * outward, climb, Math.sin(angle) * outward)
                    .normalize()
                    .add(lean);
            double magnitude = (0.10D + random.nextDouble() * 0.10D) * intensity * speedScale;

            double radius = startRadius * (0.85D + random.nextDouble() * 0.3D);
            sendParticle(level, ModParticles.WATER_PUFF.get(),
                    at.x + Math.cos(angle) * radius, at.y + 0.02D, at.z + Math.sin(angle) * radius,
                    0, velocity.x, velocity.y, velocity.z, magnitude);
        }
    }

    private static void queueWave(PendingWave wave) {
        if (PENDING_WAVES.size() >= MAX_PENDING_WAVES) return;
        PENDING_WAVES.add(wave);
    }

    // Vanilla only sends unforced particles 32 blocks, which is nothing for a rifle. Sending them per
    // player with the forced flag honours the configured distance instead.
    private static void sendParticle(ServerLevel level, ParticleOptions particle,
                                     double x, double y, double z,
                                     int count, double xd, double yd, double zd, double speed) {
        double maxDistance = TacZAdditionsConfig.COMMON.bulletWaterEffectDistance.get();
        double maxDistanceSq = maxDistance * maxDistance;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) > maxDistanceSq) continue;
            level.sendParticles(player, particle, true, x, y, z, count, xd, yd, zd, speed);
        }
    }

    // Type multipliers are deliberately mild: a shotgun should read as heavier than a pistol without
    // turning the surface hit into a different effect.
    private static double weaponScale(EntityKineticBullet bullet) {
        if (!TacZAdditionsConfig.COMMON.bulletWaterSplashByWeaponType.get()) return 1.0D;

        ResourceLocation gunId = bullet.getGunId();
        if (gunId == null) return 1.0D;

        String type = TimelessAPI.getCommonGunIndex(gunId)
                .map(CommonGunIndex::getType)
                .orElse("");

        TacZAdditionsConfig.Common config = TacZAdditionsConfig.COMMON;
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "pistol" -> config.bulletWaterSplashPistol.get();
            case "smg" -> config.bulletWaterSplashSmg.get();
            case "rifle" -> config.bulletWaterSplashRifle.get();
            case "shotgun" -> config.bulletWaterSplashShotgun.get();
            case "sniper" -> config.bulletWaterSplashSniper.get();
            case "mg", "machine_gun" -> config.bulletWaterSplashMachineGun.get();
            case "rpg" -> config.bulletWaterSplashRpg.get();
            default -> 1.0D;
        };
    }

    private static void playSplashSound(ServerLevel level, double x, double y, double z, double intensity) {
        if (!TacZAdditionsConfig.COMMON.bulletWaterSplashSound.get()) return;
        if (soundsThisTick >= MAX_SOUNDS_PER_TICK) return;

        soundsThisTick++;
        RandomSource random = level.getRandom();
        level.playSound(null, x, y, z, SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL,
                (float) (0.14D + 0.2D * intensity), 1.45F + random.nextFloat() * 0.4F);
    }

    private static boolean spend(int particles) {
        if (budgetRemaining < particles) {
            budgetRemaining = 0;
            return false;
        }
        budgetRemaining -= particles;
        return true;
    }

    private BulletWaterEffects() {}
}
