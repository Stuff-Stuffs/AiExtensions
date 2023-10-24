package io.github.stuff_stuffs.aiex.common.api.brain.memory;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.aiex.common.impl.brain.memory.MemoryReferenceImpl;

public interface MemoryReference<T> {
    MemoryType<T> type();

    static <T> Codec<MemoryReference<T>> codec(final MemoryType<T> type) {
        return MemoryReferenceImpl.codec(type);
    }
}
