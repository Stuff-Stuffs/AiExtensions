package io.github.stuff_stuffs.aiex.common.api.brain.memory;

import io.github.stuff_stuffs.aiex.common.impl.brain.memory.MemoryConfigImpl;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.Set;
import java.util.function.Supplier;

public interface MemoryConfig {
    Event<OnBuild> ON_BUILD_EVENT = EventFactory.createArrayBacked(OnBuild.class, events -> new OnBuild() {
        @Override
        public <T> void onBuild(final T entity, final Builder builder) {
            for (final OnBuild event : events) {
                event.onBuild(entity, builder);
            }
        }
    });

    Set<Memory<?>> keys();

    <T> T defaultValue(Memory<T> memory);

    static Builder builder() {
        return new MemoryConfigImpl.BuilderImpl();
    }

    interface Builder {
        boolean has(Memory<?> memory);

        <T> Supplier<? extends T> getDefaultValueFactory(Memory<T> memory);

        <T> void putDefaultValueFactory(Memory<T> memory, Supplier<? extends T> factory);

        MemoryConfig build(Object entity);
    }

    interface OnBuild {
        <T> void onBuild(T entity, Builder builder);
    }
}
