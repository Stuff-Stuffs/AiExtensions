package io.github.stuff_stuffs.aiex.common.impl.brain.memory;

import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memory;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryConfig;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class MemoryConfigImpl implements MemoryConfig {
    private final Map<Memory<?>, Supplier<?>> map;

    public MemoryConfigImpl(final Map<Memory<?>, Supplier<?>> map) {
        this.map = Map.copyOf(map);
    }

    @Override
    public Set<Memory<?>> keys() {
        return map.keySet();
    }

    @Override
    public <T> T defaultValue(final Memory<T> memory) {
        final Supplier<?> supplier = map.get(memory);
        if (supplier == null) {
            throw new NullPointerException();
        }
        //noinspection unchecked
        return (T) supplier.get();
    }

    public static final class BuilderImpl implements Builder {
        private final Map<Memory<?>, Supplier<?>> map;

        public BuilderImpl() {
            map = new Reference2ObjectOpenHashMap<>();
        }

        public BuilderImpl copy() {
            final BuilderImpl builder = new BuilderImpl();
            builder.map.putAll(map);
            return builder;
        }

        @Override
        public boolean has(final Memory<?> memory) {
            return map.containsKey(memory);
        }

        @Override
        public <T> Supplier<? extends T> getDefaultValueFactory(final Memory<T> memory) {
            final Supplier<?> supplier = map.get(memory);
            if (supplier == null) {
                throw new NullPointerException();
            }
            //noinspection unchecked
            return (Supplier<? extends T>) supplier;
        }

        @Override
        public <T> void putDefaultValueFactory(final Memory<T> memory, final Supplier<? extends T> factory) {
            map.put(memory, factory);
        }

        @Override
        public MemoryConfig build(final Object entity) {
            final BuilderImpl copy = copy();
            ON_BUILD_EVENT.invoker().onBuild(entity, copy);
            check(copy);
            return new MemoryConfigImpl(copy.map);
        }

        private static void check(final BuilderImpl builder) {
            for (final Memory<?> memory : builder.map.keySet()) {
                for (final Memory<?> dep : memory.listeningTo()) {
                    if (!builder.map.containsKey(dep)) {
                        throw new IllegalStateException();
                    }
                }
            }
        }
    }
}
