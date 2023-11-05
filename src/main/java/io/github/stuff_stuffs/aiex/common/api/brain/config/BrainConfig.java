package io.github.stuff_stuffs.aiex.common.api.brain.config;

import io.github.stuff_stuffs.aiex.common.impl.brain.BrainConfigImpl;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.util.Identifier;

import java.util.Map;

public interface BrainConfig {
    <T> T get(Key<T> key);

    static Builder builder() {
        return new BrainConfigImpl.BuilderImpl();
    }

    interface Builder {
        <T> T get(Key<T> key);

        <T> void set(Key<T> key, T value);

        BrainConfig build();
    }

    Key<Double> DEFAULT_REACH_DISTANCE = new Key<>(Double.class, 4.0);
    Key<Long> DEFAULT_UNREACHABLE_TIMEOUT = new Key<>(Long.class, 40L);

    record Key<T>(Class<T> clazz, T defaultValue) {
        private static final Map<Identifier, Key<?>> FORWARD_REGISTRY = new Object2ReferenceOpenHashMap<>();
        private static final Map<Key<?>, Identifier> BACKWARD_REGISTRY = new Reference2ObjectOpenHashMap<>();

        public static void register(final Identifier id, final Key<?> key) {
            if (FORWARD_REGISTRY.putIfAbsent(id, key) != null) {
                throw new IllegalStateException();
            }
            if (BACKWARD_REGISTRY.putIfAbsent(key, id) != null) {
                FORWARD_REGISTRY.remove(id);
                throw new IllegalStateException();
            }
        }

        public static Key<?> get(final Identifier id) {
            return FORWARD_REGISTRY.get(id);
        }

        public static Identifier getId(final Key<?> key) {
            return BACKWARD_REGISTRY.get(key);
        }

        public static void init() {
            register(AiExCommon.id("default_reach_distance"), DEFAULT_REACH_DISTANCE);
            register(AiExCommon.id("default_unreachable_timeout"), DEFAULT_UNREACHABLE_TIMEOUT);
        }
    }
}
