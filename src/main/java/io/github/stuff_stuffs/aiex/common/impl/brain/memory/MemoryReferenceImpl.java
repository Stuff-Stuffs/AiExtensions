package io.github.stuff_stuffs.aiex.common.impl.brain.memory;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryReference;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryType;

public record MemoryReferenceImpl<T>(MemoryType<T> type, long id) implements MemoryReference<T> {
    public static <T> Codec<MemoryReference<T>> codec(final MemoryType<T> type) {
        return Codec.LONG.xmap(l -> new MemoryReferenceImpl<>(type, l), t -> ((MemoryReferenceImpl<T>) t).id);
    }
}
