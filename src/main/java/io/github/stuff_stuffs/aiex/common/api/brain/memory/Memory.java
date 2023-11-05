package io.github.stuff_stuffs.aiex.common.api.brain.memory;

import java.util.Iterator;

public interface Memory<T> {
    T get();

    void set(T value);

    MemoryType<T> type();

    MemoryReference<T> reference();

    Iterator<Memory<?>> containedIn();

    boolean forgotten();

    void markDirty();
}
