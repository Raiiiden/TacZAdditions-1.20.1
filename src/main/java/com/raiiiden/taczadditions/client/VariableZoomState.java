package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.raiiiden.taczadditions.util.ScopeZoomBands;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

// Continuous scope magnification driven by the scroll wheel, kept on the client alone.
// TaCZ exposes only the discrete authored levels; this keeps a float one for the FOV hooks.
public final class VariableZoomState {

    // Magnification is multiplicative, so all stepping happens in log space: 1x to 2x should cost
    // the same scroll as 2x to 4x.
    private static final float MIN_SPAN = 1.0e-4f;

    private static boolean active;
    private static ResourceLocation scopeId;
    private static boolean builtIn;
    private static ScopeZoomBands.Bands bands;
    private static ScopeZoomBands.Band band;

    private static int trackedSlot = -1;
    private static int lastZoomNumber = Integer.MIN_VALUE;
    // Built-in scopes have no attachment NBT to hold a counter, so V cycling is tracked here.
    private static int builtInZoomNumber;

    private static float targetMagnification = 1f;
    private static float currentMagnification = 1f;

    // Detent state: while parked on an authored level, scroll accumulates here until it is large
    // enough to break out, which is what gives each level a lock the player can still push through.
    private static boolean atDetent;
    private static float detentLevel;
    private static double detentPending;

    private VariableZoomState() {
    }

    public static void reset() {
        active = false;
        scopeId = null;
        builtIn = false;
        bands = null;
        band = null;
        trackedSlot = -1;
        lastZoomNumber = Integer.MIN_VALUE;
        builtInZoomNumber = 0;
        targetMagnification = 1f;
        currentMagnification = 1f;
        clearDetent();
        ZoomHandReach.reset();
    }

    // Resolves the held scope once per tick and advances the smoothing toward the target.
    public static void tick() {
        if (!TacZAdditionsConfig.COMMON.enableVariableZoom.get()) {
            if (active) reset();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            if (active) reset();
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IGun gun)) {
            if (active) reset();
            return;
        }

        ResourceLocation resolved = gun.getAttachmentId(stack, AttachmentType.SCOPE);
        boolean resolvedBuiltIn = false;
        if (DefaultAssets.isEmptyAttachmentId(resolved)) {
            resolved = gun.getBuiltInAttachmentId(stack, AttachmentType.SCOPE);
            resolvedBuiltIn = true;
        }
        if (resolved == null || DefaultAssets.isEmptyAttachmentId(resolved)) {
            if (active) reset();
            return;
        }
        if (resolvedBuiltIn && !TacZAdditionsConfig.COMMON.variableZoomAllowBuiltInScopes.get()) {
            if (active) reset();
            return;
        }

        int slot = player.getInventory().selected;
        // A different slot or a different optic means the previous magnification no longer applies.
        boolean gunChanged = slot != trackedSlot || !resolved.equals(scopeId) || resolvedBuiltIn != builtIn;
        if (gunChanged) {
            trackedSlot = slot;
            scopeId = resolved;
            builtIn = resolvedBuiltIn;
            builtInZoomNumber = 0;
            lastZoomNumber = Integer.MIN_VALUE;
            bands = ScopeZoomBands.get(resolved);
        }

        if (bands == null || bands.isEmpty()) {
            active = false;
            band = null;
            return;
        }

        int zoomNumber = currentZoomNumber(gun, stack);
        if (zoomNumber != lastZoomNumber) {
            // Either the gun just changed, or TaCZ V-key round trip landed a new level. Either way
            // the authored level is the new target, and the smoothing animates the rest.
            lastZoomNumber = zoomNumber;
            band = bands.bandFor(zoomNumber);
            float seeded = clampToBand(bands.zoomFor(zoomNumber));
            targetMagnification = seeded;
            if (gunChanged) currentMagnification = seeded;
            // Seeding always lands exactly on an authored level, so start parked in its detent.
            // Leaving it then costs the same breakout as every other level.
            atDetent = true;
            detentLevel = seeded;
            detentPending = 0;
        }

        active = true;
        advanceSmoothing();
    }

    private static int currentZoomNumber(IGun gun, ItemStack stack) {
        if (builtIn) return builtInZoomNumber;
        CompoundTag tag = gun.getAttachmentTag(stack, AttachmentType.SCOPE);
        return tag == null ? 0 : AttachmentItemDataAccessor.getZoomNumberFromTag(tag);
    }

    private static void advanceSmoothing() {
        float speed = TacZAdditionsConfig.CLIENT.variableZoomSmoothingSpeed.get().floatValue();
        if (speed >= 1f) {
            currentMagnification = targetMagnification;
            return;
        }
        // Lerp in log space so the perceived rate is even across the range.
        double from = Math.log(currentMagnification);
        double to = Math.log(targetMagnification);
        currentMagnification = (float) Math.exp(from + (to - from) * speed);
        if (Math.abs(currentMagnification - targetMagnification) < 1.0e-4f) {
            currentMagnification = targetMagnification;
        }
    }

    // True when the scroll was consumed as a zoom change and must not reach the hotbar.
    public static boolean onScroll(double delta) {
        if (!isSweeping()) return false;
        if (delta == 0) return false;
        if (TacZAdditionsConfig.CLIENT.variableZoomInvertScroll.get()) delta = -delta;

        double span = logSpan();
        if (span < MIN_SPAN) return false;

        double sensitivity = TacZAdditionsConfig.CLIENT.variableZoomScrollSensitivity.get();
        applyStep(delta * sensitivity * span, span);
        // Only a notch that actually moved the magnification should send the hand up.
        ZoomHandReach.markAdjusting();
        return true;
    }

    private static void applyStep(double step, double span) {
        double breakout = TacZAdditionsConfig.CLIENT.variableZoomDetentBreakout.get() * span;
        // A zero breakout means the levels should not hold at all, otherwise they would still cost
        // a notch each to leave and the sweep would feel stepped rather than free.
        boolean detents = breakout > 0;
        double logTarget = Math.log(targetMagnification);

        if (atDetent && !detents) clearDetent();

        if (atDetent) {
            // Reversing out of a detent should not need the breakout already banked in the other
            // direction, so the accumulator restarts whenever the player changes their mind.
            if (detentPending != 0 && Math.signum(step) != Math.signum(detentPending)) detentPending = 0;
            detentPending += step;
            if (Math.abs(detentPending) < breakout) return;

            // Carry only the scroll beyond the breakout, so leaving a detent is not a jump.
            double remainder = detentPending - Math.signum(detentPending) * breakout;
            logTarget = Math.log(detentLevel);
            clearDetent();
            step = remainder;
        }

        double logMin = Math.log(band.min());
        double logMax = Math.log(band.max());
        double next = Mth.clamp(logTarget + step, logMin, logMax);

        // Pushing further into an end of the range moves nothing, so the level stays held rather
        // than quietly releasing and letting the next scroll away from it skip its detent.
        if (next == logTarget) {
            if (detents) {
                atDetent = true;
                detentLevel = (float) Math.exp(logTarget);
                detentPending = 0;
            }
            return;
        }

        Float caught = detents ? firstLevelBetween(logTarget, next, span) : null;
        if (caught != null) {
            atDetent = true;
            detentLevel = caught;
            detentPending = 0;
            targetMagnification = caught;
            return;
        }
        targetMagnification = (float) Math.exp(next);
    }

    // The first authored level this step reaches, either by crossing it or by landing close enough
    // to it that the detent should capture the value.
    private static Float firstLevelBetween(double from, double to, double span) {
        if (from == to) return null;
        double snap = TacZAdditionsConfig.CLIENT.variableZoomDetentSnapRange.get() * span;
        boolean ascending = to > from;

        Float best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < band.levelCount(); i++) {
            float level = band.level(i);
            double logLevel = Math.log(level);
            // Levels already sitting at the origin must not re-capture the value we are leaving.
            if (Math.abs(logLevel - from) <= 1.0e-6) continue;

            boolean crossed = ascending ? logLevel <= to : logLevel >= to;
            boolean withinSnap = Math.abs(logLevel - to) <= snap;
            if (!crossed && !withinSnap) continue;
            // Only levels lying in the direction of travel are candidates.
            if (ascending ? logLevel < from : logLevel > from) continue;

            double distance = Math.abs(logLevel - from);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = level;
            }
        }
        return best;
    }

    private static void clearDetent() {
        atDetent = false;
        detentPending = 0;
        detentLevel = 0;
    }

    private static double logSpan() {
        return Math.log(band.max()) - Math.log(band.min());
    }

    private static float clampToBand(float magnification) {
        if (band == null) return magnification;
        return Mth.clamp(magnification, band.min(), band.max());
    }

    // A variable band under an aiming local player, which is the only time scroll is taken over.
    public static boolean isSweeping() {
        if (!active || band == null || !band.isVariable()) return false;
        if (!TacZAdditionsConfig.COMMON.enableVariableZoom.get()) return false;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        return IClientPlayerGunOperator.fromLocalPlayer(player).isAim();
    }

    // Advances a built-in scope through its authored levels, standing in for the server-side cycle
    // that TaCZ skips for built-ins. Returns true when it handled the key.
    public static boolean handleBuiltInZoomKey() {
        if (!active || !builtIn || bands == null || bands.isEmpty()) return false;
        if (!TacZAdditionsConfig.COMMON.enableVariableZoom.get()) return false;
        if (!TacZAdditionsConfig.COMMON.variableZoomAllowBuiltInScopes.get()) return false;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !IClientPlayerGunOperator.fromLocalPlayer(player).isAim()) return false;

        builtInZoomNumber = Math.floorMod(builtInZoomNumber + 1, bands.levelCount());
        return true;
    }

    // The magnification the FOV hooks should use, or NaN to defer to TaCZ.
    public static float magnificationOverride(ResourceLocation queriedScopeId) {
        if (!active || band == null || scopeId == null) return Float.NaN;
        if (!TacZAdditionsConfig.COMMON.enableVariableZoom.get()) return Float.NaN;
        // Other entities guns run through the same TaCZ code path; only the local view is ours.
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getCameraEntity() != mc.player) return Float.NaN;
        if (queriedScopeId != null && !scopeId.equals(queriedScopeId)) return Float.NaN;

        float cap = TacZAdditionsConfig.COMMON.variableZoomMaxMagnification.get().floatValue();
        return Math.min(currentMagnification, cap);
    }

    // Reused so the per-frame and per-mouse-move calls below do not allocate. Consumers only read it.
    private static final float[] ZOOM_OVERRIDE = new float[1];

    // The zoom array TaCZ should see for this index, or null to leave its authored one alone.
    // Publishing here keeps view and sensitivity agreed, since TaCZ derives both from getZoom().
    public static float[] zoomArrayOverride(ClientAttachmentIndex index) {
        // The band builder reads the authored array through this very getter.
        if (ScopeZoomBands.isResolving()) return null;

        float magnification = magnificationOverride(scopeId);
        if (Float.isNaN(magnification)) return null;
        if (TimelessAPI.getClientAttachmentIndex(scopeId).orElse(null) != index) return null;

        ZOOM_OVERRIDE[0] = magnification;
        return ZOOM_OVERRIDE;
    }

    // The interpolated scope model FOV, or NaN when TaCZ own value should stand. The index is
    // checked against the tracked scope so a gun being swapped out cannot borrow this value.
    public static float viewsFovOverride(ClientAttachmentIndex index) {
        float magnification = magnificationOverride(scopeId);
        if (Float.isNaN(magnification)) return Float.NaN;
        if (!band.hasViewsFov()) return Float.NaN;
        if (index != null && TimelessAPI.getClientAttachmentIndex(scopeId).orElse(null) != index) {
            return Float.NaN;
        }
        return band.viewsFovFor(magnification);
    }

    public static float currentMagnification() {
        return currentMagnification;
    }

    // The magnification gameplay should react to: the swept value while this system owns the held
    // scope, otherwise the caller's own result, so a gun we are not tracking behaves as before.
    public static float effectiveMagnification(float taczZoom) {
        float override = magnificationOverride(scopeId);
        return Float.isNaN(override) ? taczZoom : override;
    }
}
