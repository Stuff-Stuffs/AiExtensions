package io.github.stuff_stuffs.aiex.common.impl.brain.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryReference;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryType;

public record MemoryReferenceImpl<T>(MemoryType<T> type, long id) implements MemoryReference<T> {
    public static final Codec<MemoryReferenceImpl<?>> ANY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MemoryType.CODEC.fieldOf("type").forGetter(MemoryReferenceImpl::type),
            Codec.LONG.fieldOf("id").forGetter(MemoryReferenceImpl::id)
    ).apply(instance, MemoryReferenceImpl::new));

    public static <T> Codec<MemoryReference<T>> codec(final MemoryType<T> type) {
        return Codec.LONG.xmap(l -> new MemoryReferenceImpl<>(type, l), t -> ((MemoryReferenceImpl<T>) t).id);
    }
}
