package com.raiiiden.taczadditions.event;

import com.tacz.guns.client.event.CameraSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.lang.reflect.Field;

public class RecoilDisabler {

    public static void disableRecoil(FMLClientSetupEvent event) {
        try {
            // Get access to the private fields controlling recoil recovery
            Field pitchField = CameraSetupEvent.class.getDeclaredField("pitchSplineFunction");
            Field yawField = CameraSetupEvent.class.getDeclaredField("yawSplineFunction");

            pitchField.setAccessible(true);
            yawField.setAccessible(true);

            // Disable recoil recovery by setting them to null
            pitchField.set(null, null);
            yawField.set(null, null);

            System.out.println("[TaczAdditions] Recoil recovery disabled!");

        } catch (Exception e) {
            System.err.println("[TaczAdditions] Failed to disable recoil recovery!");
            e.printStackTrace();
        }
    }
}
