package io.github.stuff_stuffs.aiex.common.api.brain.node.basic;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryName;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.function.BiFunction;

public class NamedRememberingBrainNode<C, M, FC> implements BrainNode<C, M, FC> {
    private final BiFunction<BrainContext<C>, FC, MemoryName<M>> nameExtractor;
    private final BiFunction<BrainContext<C>, FC, M> memoryExtractor;

    public NamedRememberingBrainNode(final MemoryName<M> name, final BiFunction<BrainContext<C>, FC, M> memoryExtractor) {
        this((context, arg) -> name, memoryExtractor);
    }

    public NamedRememberingBrainNode(final BiFunction<BrainContext<C>, FC, MemoryName<M>> nameExtractor, final BiFunction<BrainContext<C>, FC, M> memoryExtractor) {
        this.nameExtractor = nameExtractor;
        this.memoryExtractor = memoryExtractor;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {

    }

    @Override
    public M tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
        final MemoryName<M> loc = nameExtractor.apply(context, arg);
        final M memory = memoryExtractor.apply(context, arg);
        context.brain().memories().put(loc, memory);
        return memory;
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {

    }
}
