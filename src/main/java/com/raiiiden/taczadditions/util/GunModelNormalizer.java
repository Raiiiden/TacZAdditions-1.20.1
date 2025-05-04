package com.raiiiden.taczadditions.util;

import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;

public class GunModelNormalizer {

    public static void normalizeGunModel(BedrockGunModel model) {
        BedrockPart root = model.getRootNode();
        if (root != null) {
            // Adjust offsets to a standard reference point
            root.offsetX -= root.x;
            root.offsetY -= root.y;
            root.offsetZ -= root.z;

            // Reset positions to zero
            root.x = 0;
            root.y = 0;
            root.z = 0;
        }
    }
}