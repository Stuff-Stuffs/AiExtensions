package io.github.stuff_stuffs.aiex.common.api.brain.node.basic.memory;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memory;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryReference;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MemoryLoadBrainNode<C, M, FC> implements BrainNode<C, Optional<Memory<M>>, FC> {
    private final BiFunction<BrainContext<C>, FC, MemoryReference<M>> memoryExtractor;

    public MemoryLoadBrainNode(final Function<FC, MemoryReference<M>> extractor) {
        this((context, arg) -> extractor.apply(arg));
    }

    public MemoryLoadBrainNode(final BiFunction<BrainContext<C>, FC, MemoryReference<M>> extractor) {
        memoryExtractor = extractor;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {

    }

    @Override
    public Optional<Memory<M>> tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
        final MemoryReference<M> reference = memoryExtractor.apply(context, arg);
        return context.brain().memories().get(reference);
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {

    }
}
