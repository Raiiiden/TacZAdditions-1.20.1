package com.raiiiden.taczadditions.util;

import org.joml.Vector3f;

public class AnimationState {
    public Vector3f originalMagPos = new Vector3f();
    public Vector3f originalMagRot = new Vector3f();
    public Vector3f originalLeftHandPos = new Vector3f();
    public Vector3f originalLeftHandRot = new Vector3f();
    public long startTime = 0;
    public boolean isAnimating = false;
}