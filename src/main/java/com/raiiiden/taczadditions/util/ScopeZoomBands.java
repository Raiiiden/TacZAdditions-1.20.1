package com.raiiiden.taczadditions.util;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Splits a scope's authored zoom levels into continuous magnification bands.
//
// TaCZ keeps a scope's magnifications in a flat zoom[] array indexed by the ZoomNumber NBT tag, but
// the entries do not all belong to the same optic. views[] selects which sight model is rendered:
// FirstPersonRenderGunEvent maps views[i] - 1 onto BedrockAttachmentModel#getScopeViewPath. A run of
// equal views[] values is therefore one physical sight whose magnifications can be swept smoothly,
// while a change in views[] is a jump to a different sight and has to stay a discrete step.
//
// scope_lpvo_1_6 has zoom [6.25, 1.25] with views [2, 2]: one band, a real 1.25-6.25x variable optic.
// scope_vudu has zoom [6.5, 1.35] with views [2, 1]: two single-level bands, a 6.5x scope with a
// separate 1.35x backup sight, which must not be swept.
public final class ScopeZoomBands {

    // A run of authored levels that share a sight, sorted ascending by magnification.
    public static final class Band {
        private final float[] zooms;
        private final float[] viewsFov;
        private final boolean hasViewsFov;

        private Band(float[] zooms, float[] viewsFov, boolean hasViewsFov) {
            this.zooms = zooms;
            this.viewsFov = viewsFov;
            this.hasViewsFov = hasViewsFov;
        }

        // False when the scope authored no usable views_fov. Callers must then leave the model FOV
        // to TaCZ instead of substituting a value of their own.
        public boolean hasViewsFov() {
            return hasViewsFov;
        }

        // Only a run holding more than one distinct magnification can be swept.
        public boolean isVariable() {
            return zooms.length > 1;
        }

        public float min() {
            return zooms[0];
        }

        public float max() {
            return zooms[zooms.length - 1];
        }

        public int levelCount() {
            return zooms.length;
        }

        public float level(int i) {
            return zooms[i];
        }

        // The scope model FOV for an arbitrary magnification, interpolated piecewise-linearly over
        // log(zoom) between the authored points so the scope picture tracks evenly across the range.
        public float viewsFovFor(float magnification) {
            if (!hasViewsFov) return Float.NaN;
            if (zooms.length == 1 || magnification <= zooms[0]) return viewsFov[0];
            int last = zooms.length - 1;
            if (magnification >= zooms[last]) return viewsFov[last];

            for (int i = 1; i <= last; i++) {
                if (magnification > zooms[i]) continue;
                double lo = Math.log(zooms[i - 1]);
                double hi = Math.log(zooms[i]);
                double t = hi > lo ? (Math.log(magnification) - lo) / (hi - lo) : 0.0;
                return (float) (viewsFov[i - 1] + (viewsFov[i] - viewsFov[i - 1]) * t);
            }
            return viewsFov[last];
        }
    }

    // The full band layout for one scope, plus the authored-order lookup the ZoomNumber tag indexes.
    public static final class Bands {
        private final Band[] bands;
        private final int[] indexToBand;
        private final float[] indexToZoom;

        private Bands(Band[] bands, int[] indexToBand, float[] indexToZoom) {
            this.bands = bands;
            this.indexToBand = indexToBand;
            this.indexToZoom = indexToZoom;
        }

        public boolean isEmpty() {
            return indexToBand.length == 0;
        }

        public int levelCount() {
            return indexToBand.length;
        }

        // ZoomNumber is a free-running counter, so it is wrapped here exactly as TaCZ wraps it.
        public Band bandFor(int zoomNumber) {
            if (isEmpty()) return null;
            return bands[indexToBand[Math.floorMod(zoomNumber, indexToBand.length)]];
        }

        public float zoomFor(int zoomNumber) {
            if (isEmpty()) return 1f;
            return indexToZoom[Math.floorMod(zoomNumber, indexToZoom.length)];
        }
    }

    private static final Bands EMPTY = new Bands(new Band[0], new int[0], new float[0]);

    private static final Map<ResourceLocation, Bands> CACHE = new HashMap<>();

    // Identity of the config lists the cache was built against. Both reloads and server sync hand
    // back a fresh list instance, which is the same trick ConfiguredBlockSet uses.
    private static List<? extends String> cachedForceVariable;
    private static List<? extends String> cachedForceFixed;
    private static Set<ResourceLocation> forceVariable = Set.of();
    private static Set<ResourceLocation> forceFixed = Set.of();

    // Set while a layout is being read off an index. The continuous zoom is published by overriding
    // ClientAttachmentIndex#getZoom, so the builder has to be able to see the real authored array.
    private static boolean resolving;

    private ScopeZoomBands() {
    }

    public static boolean isResolving() {
        return resolving;
    }

    public static Bands get(ResourceLocation scopeId) {
        if (scopeId == null) return EMPTY;
        refreshOverridesIfChanged();
        Bands cached = CACHE.get(scopeId);
        if (cached != null) return cached;

        resolving = true;
        try {
            Bands built = TimelessAPI.getClientAttachmentIndex(scopeId)
                    .map(index -> build(scopeId, index))
                    .orElse(EMPTY);
            CACHE.put(scopeId, built);
            return built;
        } finally {
            resolving = false;
        }
    }

    public static void clearCache() {
        CACHE.clear();
        cachedForceVariable = null;
        cachedForceFixed = null;
    }

    private static void refreshOverridesIfChanged() {
        List<? extends String> variable = TacZAdditionsConfig.COMMON.variableZoomForceVariableScopes.get();
        List<? extends String> fixed = TacZAdditionsConfig.COMMON.variableZoomForceFixedScopes.get();
        if (variable == cachedForceVariable && fixed == cachedForceFixed) return;

        cachedForceVariable = variable;
        cachedForceFixed = fixed;
        forceVariable = parseIds(variable);
        forceFixed = parseIds(fixed);
        CACHE.clear();
    }

    private static Set<ResourceLocation> parseIds(List<? extends String> entries) {
        Set<ResourceLocation> ids = new HashSet<>();
        for (String entry : entries) {
            if (entry == null) continue;
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            ResourceLocation id = ResourceLocation.tryParse(trimmed);
            if (id != null) ids.add(id);
        }
        return ids;
    }

    private static Bands build(ResourceLocation scopeId, ClientAttachmentIndex index) {
        float[] zoom = index.getZoom();
        if (zoom == null || zoom.length == 0) return EMPTY;

        int[] views = index.getViews();
        float[] viewsFov = index.getViewsFov();
        // views_fov is optional, and a scope may author fewer entries than zooms. Falling back to a
        // flat profile keeps the interpolation a no-op instead of failing on an index.
        boolean fovUsable = viewsFov != null && viewsFov.length == zoom.length;

        int[] group = new int[zoom.length];
        if (forceFixed.contains(scopeId)) {
            // Every level becomes its own band, so nothing is ever swept.
            for (int i = 0; i < zoom.length; i++) group[i] = i;
        } else if (forceVariable.contains(scopeId) || views == null || views.length != zoom.length) {
            // Without usable views data we cannot tell sights apart, so treat the scope as one optic.
            // That is a no-op for the single-level scopes, which are the ones that omit views.
            for (int i = 0; i < zoom.length; i++) group[i] = 0;
        } else {
            int current = 0;
            for (int i = 0; i < zoom.length; i++) {
                if (i > 0 && views[i] != views[i - 1]) current++;
                group[i] = current;
            }
        }

        int groupCount = 0;
        for (int g : group) groupCount = Math.max(groupCount, g + 1);

        Band[] bands = new Band[groupCount];
        for (int g = 0; g < groupCount; g++) {
            List<Integer> members = new ArrayList<>();
            for (int i = 0; i < group.length; i++) {
                if (group[i] == g) members.add(i);
            }
            // Authored order is arbitrary, an LPVO lists 6.25 before 1.25, so sort by magnification
            // and carry each level's model FOV along with it.
            members.sort((a, b) -> Float.compare(zoom[a], zoom[b]));

            List<Float> zooms = new ArrayList<>();
            List<Float> fovs = new ArrayList<>();
            for (int i : members) {
                // A duplicate magnification would create a zero-width interpolation segment.
                if (!zooms.isEmpty() && zooms.get(zooms.size() - 1) == zoom[i]) continue;
                zooms.add(zoom[i]);
                fovs.add(fovUsable ? viewsFov[i] : 0f);
            }
            bands[g] = new Band(toFloatArray(zooms), toFloatArray(fovs), fovUsable);
        }

        int[] indexToBand = new int[zoom.length];
        float[] indexToZoom = new float[zoom.length];
        for (int i = 0; i < zoom.length; i++) {
            indexToBand[i] = group[i];
            indexToZoom[i] = zoom[i];
        }
        return new Bands(bands, indexToBand, indexToZoom);
    }

    private static float[] toFloatArray(List<Float> values) {
        float[] out = new float[values.size()];
        for (int i = 0; i < out.length; i++) out[i] = values.get(i);
        return out;
    }
}
