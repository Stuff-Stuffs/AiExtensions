package io.github.stuff_stuffs.aiex.common.api.brain.task.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.function.BiFunction;

public class FallthroughChainedBrainNode<C, R0, FC0, FC1, R1> implements BrainNode<C, R1, FC0> {
    private final BrainNode<C, R0, FC0> first;
    private final BrainNode<C, R1, FC1> second;
    private final BiFunction<R0, FC0, FC1> combiner;

    public FallthroughChainedBrainNode(final BrainNode<C, R0, FC0> first, final BrainNode<C, R1, FC1> second, final BiFunction<R0, FC0, FC1> combiner) {
        this.first = first;
        this.second = second;
        this.combiner = combiner;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("FallthroughChain")) {
            first.init(context, child);
            second.init(context, child);
        }
    }

    @Override
    public R1 tick(final BrainContext<C> context, final FC0 arg, final SpannedLogger logger) {
        try (final var child = logger.open("FallthroughChain")) {
            final R0 firstRes = first.tick(context, arg, child);
            child.debug("Fallthrough chaining!");
            return second.tick(context, combiner.apply(firstRes, arg), child);
        }
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("FallthroughChain")) {
            first.deinit(context, child);
            second.deinit(context, child);
        }
    }
}
