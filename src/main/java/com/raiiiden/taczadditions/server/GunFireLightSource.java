package com.raiiiden.taczadditions.server;

import atomicstryker.dynamiclights.server.IDynamicLightSource;
import net.minecraft.world.entity.Entity;

public class GunFireLightSource implements IDynamicLightSource {
    private Entity attachmentEntity;
    private int lightLevel;
    private int ticksAlive;
    private static final int LIGHT_DURATION_TICKS = 2;

    public GunFireLightSource(Entity entity, int lightLevel) {
        this.attachmentEntity = entity;
        this.lightLevel = lightLevel;
        this.ticksAlive = 0;
    }

    public void updateLight(int newLightLevel) {
        this.lightLevel = newLightLevel;
        this.ticksAlive = 0;
    }

    public void tick() {
        ticksAlive++;
    }

    @Override
    public Entity getAttachmentEntity() {
        return attachmentEntity;
    }

    @Override
    public int getLightLevel() {
        return ticksAlive >= LIGHT_DURATION_TICKS ? 0 : lightLevel;
    }
}