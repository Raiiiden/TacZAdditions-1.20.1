package com.raiiiden.taczadditions.config;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Synchronizes common config values by path so unknown settings can be ignored.
public final class ConfigSync {
    private static final Logger LOGGER = LogManager.getLogger();

    public enum Kind {
        BOOLEAN,
        DOUBLE,
        INTEGER,
        STRING_LIST
    }

    private static final Map<String, SyncedValue<?>> ENTRIES = new LinkedHashMap<>();

    private ConfigSync() {
    }

    static SyncedValue<Boolean> bool(String path, ForgeConfigSpec.BooleanValue value) {
        return register(new SyncedValue<>(path, Kind.BOOLEAN, value));
    }

    static SyncedValue<Double> dbl(String path, ForgeConfigSpec.DoubleValue value) {
        return register(new SyncedValue<>(path, Kind.DOUBLE, value));
    }

    static SyncedValue<Integer> integer(String path, ForgeConfigSpec.IntValue value) {
        return register(new SyncedValue<>(path, Kind.INTEGER, value));
    }

    static SyncedValue<List<? extends String>> stringList(String path,
                                                          ForgeConfigSpec.ConfigValue<List<? extends String>> value) {
        return register(new SyncedValue<>(path, Kind.STRING_LIST, value));
    }

    private static <T> SyncedValue<T> register(SyncedValue<T> value) {
        SyncedValue<?> previous = ENTRIES.put(value.path(), value);
        if (previous != null) {
            throw new IllegalStateException("Duplicate synced config path: " + value.path());
        }
        return value;
    }

    // Builds a snapshot from local values.
    public static Map<String, Object> snapshotLocalValues() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        for (SyncedValue<?> entry : ENTRIES.values()) {
            snapshot.put(entry.path(), entry.getLocal());
        }
        return snapshot;
    }

    // Applies a server snapshot and ignores unknown paths.
    public static void applyServerValues(Map<String, Object> values) {
        // Replace the entire previous snapshot.
        releaseServerValues();

        int applied = 0;
        for (Map.Entry<String, Object> received : values.entrySet()) {
            SyncedValue<?> entry = ENTRIES.get(received.getKey());
            if (entry == null) continue;
            entry.lock(received.getValue());
            applied++;
        }
        LOGGER.debug("Locked {} config values to the connected server", applied);
    }

    // Restores local values after disconnecting.
    public static void releaseServerValues() {
        for (SyncedValue<?> entry : ENTRIES.values()) {
            entry.unlock();
        }
    }

    public static void write(FriendlyByteBuf buffer, Map<String, Object> values) {
        buffer.writeVarInt(values.size());
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            SyncedValue<?> known = ENTRIES.get(entry.getKey());
            buffer.writeUtf(entry.getKey());
            // Kind travels with the value so a receiver that does not know the path can still skip it.
            Kind kind = known == null ? kindOf(entry.getValue()) : known.kind();
            buffer.writeByte(kind.ordinal());
            writeValue(buffer, kind, entry.getValue());
        }
    }

    public static Map<String, Object> read(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String path = buffer.readUtf(256);
            int kindId = buffer.readUnsignedByte();
            Kind[] kinds = Kind.values();
            if (kindId >= kinds.length) {
                throw new IllegalArgumentException("Unknown config value type " + kindId + " for " + path);
            }
            Kind kind = kinds[kindId];
            Object value = readValue(buffer, kind);

            SyncedValue<?> known = ENTRIES.get(path);
            // Reject known paths with incompatible types.
            if (known != null && known.kind() != kind) {
                LOGGER.warn("Ignoring config value {}: server sent {} but this version expects {}",
                        path, kind, known.kind());
                continue;
            }
            if (value != null) values.put(path, value);
        }
        return values;
    }

    private static Kind kindOf(Object value) {
        if (value instanceof Boolean) return Kind.BOOLEAN;
        if (value instanceof Integer) return Kind.INTEGER;
        if (value instanceof Number) return Kind.DOUBLE;
        return Kind.STRING_LIST;
    }

    private static void writeValue(FriendlyByteBuf buffer, Kind kind, Object value) {
        switch (kind) {
            case BOOLEAN -> buffer.writeBoolean((Boolean) value);
            case DOUBLE -> buffer.writeDouble(((Number) value).doubleValue());
            case INTEGER -> buffer.writeVarInt(((Number) value).intValue());
            case STRING_LIST -> {
                List<?> list = (List<?>) value;
                buffer.writeVarInt(list.size());
                for (Object element : list) {
                    buffer.writeUtf(String.valueOf(element), 256);
                }
            }
        }
    }

    private static Object readValue(FriendlyByteBuf buffer, Kind kind) {
        switch (kind) {
            case BOOLEAN -> {
                return buffer.readBoolean();
            }
            case DOUBLE -> {
                double value = buffer.readDouble();
                // A non-finite value would poison every angle computed from it.
                return Double.isFinite(value) ? value : null;
            }
            case INTEGER -> {
                return buffer.readVarInt();
            }
            default -> {
                int size = buffer.readVarInt();
                List<String> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(buffer.readUtf(256));
                }
                return List.copyOf(list);
            }
        }
    }
}
