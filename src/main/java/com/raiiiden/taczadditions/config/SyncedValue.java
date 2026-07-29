package com.raiiiden.taczadditions.config;

import net.minecraftforge.common.ForgeConfigSpec;

// Common config value that can be overridden by a connected server.
public final class SyncedValue<T> {
    private final String path;
    private final ConfigSync.Kind kind;
    private final ForgeConfigSpec.ConfigValue<T> local;

    // Written from the network thread's enqueued work, read from render and tick threads.
    private volatile T override;

    SyncedValue(String path, ConfigSync.Kind kind, ForgeConfigSpec.ConfigValue<T> local) {
        this.path = path;
        this.kind = kind;
        this.local = local;
    }

    // Returns the server override when one is active.
    public T get() {
        T locked = override;
        return locked != null ? locked : local.get();
    }

    // Returns this installation's local value.
    public T getLocal() {
        return local.get();
    }

    public boolean isLockedByServer() {
        return override != null;
    }

    public String path() {
        return path;
    }

    ConfigSync.Kind kind() {
        return kind;
    }

    @SuppressWarnings("unchecked")
    void lock(Object value) {
        this.override = (T) value;
    }

    void unlock() {
        this.override = null;
    }
}
