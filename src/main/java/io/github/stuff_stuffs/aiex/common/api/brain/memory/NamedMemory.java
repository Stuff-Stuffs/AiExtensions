package io.github.stuff_stuffs.aiex.common.api.brain.memory;

public interface NamedMemory<T> extends Memory<T> {
    MemoryName<T> name();
}
