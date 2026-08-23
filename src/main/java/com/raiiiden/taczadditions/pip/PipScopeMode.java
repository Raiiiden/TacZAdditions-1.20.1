package com.raiiiden.taczadditions.pip;

// How a magnified optic shows its image.
public enum PipScopeMode {
    // Narrows the whole screen FOV, which is what TaCZ does on its own.
    OFF,
    // Crops the frame that was already drawn into the lens. Cheap, but no sharper than the screen.
    FAKE,
    // Draws the world a second time through the scope for true magnification.
    REAL
}
