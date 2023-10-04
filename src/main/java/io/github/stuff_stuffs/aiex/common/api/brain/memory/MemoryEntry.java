package io.github.stuff_stuffs.aiex.common.api.brain.memory;

public interface MemoryEntry<T> {
    Memory<T> type();

    T get();

    void set(T value);
}
