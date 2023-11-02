package io.github.stuff_stuffs.aiex.common.api.brain.node.basic;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memory;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryName;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.Optional;
import java.util.function.BiFunction;

public class NamedForgettingNode<C, M, FC> implements BrainNode<C, Optional<M>, FC> {
    private final BiFunction<BrainContext<C>, FC, MemoryName<M>> nameExtractor;

    public NamedForgettingNode(final MemoryName<M> name) {
        this((context, arg) -> name);
    }

    public NamedForgettingNode(final BiFunction<BrainContext<C>, FC, MemoryName<M>> extractor) {
        nameExtractor = extractor;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {

    }

    @Override
    public Optional<M> tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
        final MemoryName<M> name = nameExtractor.apply(context, arg);
        final Optional<Memory<M>> memory = context.brain().memories().get(name);
        if (memory.isEmpty()) {
            return Optional.empty();
        }
        final Optional<M> ret = memory.map(Memory::get);
        if (!context.brain().memories().forget(name)) {
            return Optional.empty();
        }
        return ret;
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {

    }
}
