package io.github.stuff_stuffs.aiex.common.impl.brain.memory;

import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryName;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryType;

public class MemoryNameImpl<T> implements MemoryName<T> {
    private final MemoryType<T> type;

    public MemoryNameImpl(final MemoryType<T> type) {
        this.type = type;
    }

    @Override
    public MemoryType<T> type() {
        return type;
    }
}
