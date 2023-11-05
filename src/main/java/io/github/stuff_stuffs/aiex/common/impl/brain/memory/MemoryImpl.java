package io.github.stuff_stuffs.aiex.common.impl.brain.memory;

import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memory;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryReference;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryType;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.TickingMemory;

import java.util.Iterator;
import java.util.function.Consumer;

public class MemoryImpl<T> implements Memory<T> {
    private final MemoryType<T> type;
    private final MemoriesImpl parent;
    private final long id;
    private final Consumer<TickingMemory> addTickable;
    private final Runnable removeTickable;
    private T value;
    private boolean forgotten = false;

    public MemoryImpl(final MemoryType<T> type, final T value, final long id, final MemoriesImpl parent, final Consumer<TickingMemory> addTickable, final Runnable removeTickable) {
        this.type = type;
        this.id = id;
        this.parent = parent;
        this.value = value;
        this.addTickable = addTickable;
        this.removeTickable = removeTickable;
        if (value instanceof TickingMemory tickingMemory) {
            addTickable.accept(tickingMemory);
        }
    }

    public long id() {
        return id;
    }

    public void forget() {
        forgotten = true;
        removeTickable.run();
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
        if (value instanceof TickingMemory tickingMemory) {
            addTickable.accept(tickingMemory);
        } else {
            removeTickable.run();
        }
        this.value = value;
        parent.change(id, this);
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

    @Override
    public void markDirty() {
        parent.change(id, this);
    }
}
