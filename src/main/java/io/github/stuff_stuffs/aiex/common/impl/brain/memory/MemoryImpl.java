package io.github.stuff_stuffs.aiex.common.impl.brain.memory;

import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memory;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryReference;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryType;

import java.util.Iterator;

public class MemoryImpl<T> implements Memory<T> {
    private final MemoryType<T> type;
    private final MemoriesImpl parent;
    private final long id;
    private T value;
    private boolean forgotten = false;

    public MemoryImpl(final MemoryType<T> type, final T value, final long id, final MemoriesImpl parent) {
        this.type = type;
        this.id = id;
        this.parent = parent;
        this.value = value;
    }

    public long id() {
        return id;
    }

    public void forget() {
        forgotten = true;
    }

    @Override
    public T get() {
        if (forgotten) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void set(final T value) {
        if (value == null) {
            throw new NullPointerException();
        }
        if (forgotten) {
            throw new IllegalStateException();
        }
        final T old = this.value;
        this.value = value;
        parent.change(id, this, old);
    }

    @Override
    public MemoryType<T> type() {
        return type;
    }

    @Override
    public MemoryReference<T> reference() {
        return new MemoryReferenceImpl<>(type, id);
    }

    @Override
    public Iterator<Memory<?>> containedIn() {
        if (forgotten) {
            throw new IllegalStateException();
        }
        return parent.containedIn(id);
    }

    @Override
    public boolean forgotten() {
        return forgotten;
    }
}
