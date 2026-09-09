package com.raiiiden.taczadditions.pip;

import com.raiiiden.taczadditions.TaczAdditions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// The scope owns framebuffers and textures, which have to go back to the driver when the level
// they were allocated for is gone.
@Mod.EventBusSubscriber(modid = TaczAdditions.MODID, value = Dist.CLIENT)
public final class PipScopeLifecycle {

    private PipScopeLifecycle() {
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            PipScopeRenderer.cleanup();
        }
    }
}
