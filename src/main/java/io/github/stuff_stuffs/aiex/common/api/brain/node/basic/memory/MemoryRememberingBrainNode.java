package io.github.stuff_stuffs.aiex.common.api.brain.node.basic.memory;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryReference;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryType;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.function.BiFunction;
import java.util.function.Function;

public class MemoryRememberingBrainNode<C, M, FC> implements BrainNode<C, MemoryReference<M>, FC> {
    private final MemoryType<M> type;
    private final BiFunction<BrainContext<C>, FC, M> memoryExtractor;

    public MemoryRememberingBrainNode(final Function<FC, M> extractor, final MemoryType<M> type) {
        this(type, (context, arg) -> extractor.apply(arg));
    }

    public MemoryRememberingBrainNode(final MemoryType<M> type, final BiFunction<BrainContext<C>, FC, M> extractor) {
        this.type = type;
        memoryExtractor = extractor;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {

    }

    @Override
    public MemoryReference<M> tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
        final M memory = memoryExtractor.apply(context, arg);
        return context.brain().memories().add(type, memory);
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {

    }
}
