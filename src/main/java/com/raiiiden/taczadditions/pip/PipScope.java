package com.raiiiden.taczadditions.pip;

import com.raiiiden.taczadditions.config.TacZAdditionsConfig;
import com.tacz.guns.compat.oculus.OculusCompat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The mode actually in force, which is the player's own choice unless the server withheld it.
@OnlyIn(Dist.CLIENT)
public final class PipScope {

    private static final Logger LOGGER = LogManager.getLogger();

    // Said once a session, not once a frame.
    private static boolean warnedFakeUnderShaders;

    private PipScope() {
    }

    public static PipScopeMode mode() {
        if (!TacZAdditionsConfig.COMMON.enablePipScope.get()) return PipScopeMode.OFF;
        PipScopeMode mode = TacZAdditionsConfig.CLIENT.pipScopeMode.get();
        if (mode == null) return PipScopeMode.OFF;
        if (mode == PipScopeMode.FAKE && OculusCompat.isUsingRenderPack()) {
            warnFakeUnderShaders();
        }
        return mode;
    }

    // FAKE needs a finished frame with no gun in it, and under a shader pack none exists: the pack
    // draws the held item inside the level render, so the crop fills the lens with the eyepiece.
    private static void warnFakeUnderShaders() {
        if (warnedFakeUnderShaders) return;
        warnedFakeUnderShaders = true;
        LOGGER.warn("Picture-in-picture scopes are set to FAKE while a shader pack is loaded. "
                + "FAKE crops the frame the game already drew, and with a shader pack there is no "
                + "point in the frame where the world is finished but the gun has not been drawn "
                + "yet, so the lens will look wrong. Use REAL for scopes under shaders, or OFF.");
    }

    public static boolean enabled() {
        return mode() != PipScopeMode.OFF;
    }
}
