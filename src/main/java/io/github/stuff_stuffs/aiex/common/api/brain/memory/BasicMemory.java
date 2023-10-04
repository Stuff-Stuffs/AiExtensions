package io.github.stuff_stuffs.aiex.common.api.brain.memory;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class BasicMemory<T> implements Memory<T> {
    private final Codec<T> codec;
    private final Map<Memory<?>, Updater<T, ?>> updateMap;
    private final Map<Memory<?>, Updater<T, ?>> optionalUpdateMap;

    private BasicMemory(final Codec<T> codec, final Map<Memory<?>, Updater<T, ?>> updateMap, final Map<Memory<?>, Updater<T, ?>> optionalUpdateMap) {
        this.codec = codec;
        this.updateMap = Map.copyOf(updateMap);
        this.optionalUpdateMap = Map.copyOf(optionalUpdateMap);
    }

    @Override
    public Codec<T> codec() {
        return codec;
    }

    @Override
    public Set<Memory<?>> listeningTo() {
        return Collections.unmodifiableSet(updateMap.keySet());
    }

    @Override
    public Set<Memory<?>> optionalListeningTo() {
        return Collections.unmodifiableSet(optionalUpdateMap.keySet());
    }

    @Override
    public <T0> T listenerMemoryUpdate(final Memory<T0> memory, final T0 oldValue, final T0 newValue, final T currentVal, final AiBrainView brain) {
        if (updateMap.containsKey(memory)) {
            //noinspection unchecked
            final Updater<T, T0> updater = (Updater<T, T0>) updateMap.get(memory);
            return updater.update(oldValue, newValue, currentVal, brain);
        } else if (optionalUpdateMap.containsKey(memory)) {
            //noinspection unchecked
            final Updater<T, T0> updater = (Updater<T, T0>) optionalUpdateMap.get(memory);
            return updater.update(oldValue, newValue, currentVal, brain);
        }
        return currentVal;
    }

    public interface Updater<T0, T1> {
        T0 update(T1 oldValue, T1 newValue, T0 value, AiBrainView brain);
    }

    public static <T> Builder<T> builder(final Codec<T> codec) {
        return new Builder<>(codec);
    }

    public static final class Builder<T> {
        private final Codec<T> codec;
        private final Map<Memory<?>, Updater<T, ?>> updateMap;
        private final Map<Memory<?>, Updater<T, ?>> optionalUpdateMap;

        private Builder(final Codec<T> codec) {
            this.codec = codec;
            updateMap = new Reference2ObjectOpenHashMap<>();
            optionalUpdateMap = new Reference2ObjectOpenHashMap<>();
        }

        public <T0> Builder<T> add(final Memory<T0> memory, final Updater<T, T0> updater) {
            updateMap.put(memory, updater);
            return this;
        }

        public <T0> Builder<T> addOpt(final Memory<T0> memory, final Updater<T, T0> updater) {
            optionalUpdateMap.put(memory, updater);
            return this;
        }

        public BasicMemory<T> build() {
            return new BasicMemory<>(codec, updateMap, optionalUpdateMap);
        }
    }
}
