package com.raiiiden.taczadditions.client;

import com.raiiiden.taczadditions.TaczAdditions;

import java.lang.reflect.Method;

// Reflection bridge to Colorful Lighting's optional RGB type.
public final class ColorfulLightingBridge {
    private static final String COLOR_CLASS =
            "me.erykczy.colorfullighting.common.util.ColorRGB4";

    private static boolean initialized;
    private static Method fromRgb4;

    private ColorfulLightingBridge() {
    }

    public static Object createColor(int rgb888) {
        if (rgb888 < 0) return null;
        if (!initialized) initialize();
        if (fromRgb4 == null) return null;

        int red = toFourBit((rgb888 >>> 16) & 0xFF);
        int green = toFourBit((rgb888 >>> 8) & 0xFF);
        int blue = toFourBit(rgb888 & 0xFF);
        try {
            return fromRgb4.invoke(null, red, green, blue);
        } catch (ReflectiveOperationException | LinkageError error) {
            fromRgb4 = null;
            TaczAdditions.LOGGER.warn(
                    "[TacZAdditions] Colorful Lighting color creation failed; "
                            + "colored muzzle flashes are disabled",
                    error);
            return null;
        }
    }

    private static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> colorClass = Class.forName(
                    COLOR_CLASS, false, ColorfulLightingBridge.class.getClassLoader());
            fromRgb4 = colorClass.getMethod("fromRGB4", int.class, int.class, int.class);
            TaczAdditions.LOGGER.info(
                    "[TacZAdditions] Colorful Lighting muzzle-flash bridge initialized");
        } catch (ReflectiveOperationException | LinkageError error) {
            fromRgb4 = null;
            TaczAdditions.LOGGER.warn(
                    "[TacZAdditions] Colorful Lighting was detected but its color API is incompatible; "
                            + "colored muzzle flashes are disabled",
                    error);
        }
    }

    private static int toFourBit(int channel) {
        return (channel * 15 + 127) / 255;
    }
}
