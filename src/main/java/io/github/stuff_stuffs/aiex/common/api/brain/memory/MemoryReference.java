package io.github.stuff_stuffs.aiex.common.api.brain.memory;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.github.stuff_stuffs.aiex.common.impl.brain.memory.MemoryReferenceImpl;

public interface MemoryReference<T> {
    Codec<MemoryReference<?>> ANY_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<MemoryReference<?>, T>> decode(final DynamicOps<T> ops, final T input) {
            return MemoryReferenceImpl.ANY_CODEC.decode(ops, input).map(p -> Pair.of(p.getFirst(), p.getSecond()));
        }

        @Override
        public <T> DataResult<T> encode(final MemoryReference<?> input, final DynamicOps<T> ops, final T prefix) {
            if (input instanceof final MemoryReferenceImpl<?> impl) {
                return MemoryReferenceImpl.ANY_CODEC.encode(impl, ops, prefix);
            }
            return DataResult.error(() -> "Somebody implemented an internal interface!");
        }
    };

    MemoryType<T> type();

    static <T> Codec<MemoryReference<T>> codec(final MemoryType<T> type) {
        return MemoryReferenceImpl.codec(type);
    }
}
