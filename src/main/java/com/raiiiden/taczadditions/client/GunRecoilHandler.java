package com.raiiiden.taczadditions.client;

public class GunRecoilHandler {
    public static float recoilX = 0;
    public static float recoilY = 0;
    public static float recoilZ = 0;
    public static long lastRecoilTime = 0;

    public static void trigger(float x, float y, float z) {
        recoilX = x;
        recoilY = y;
        recoilZ = z;
        lastRecoilTime = System.currentTimeMillis();
    }
}
