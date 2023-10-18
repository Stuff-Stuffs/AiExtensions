package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

public class ChainedBrainNode<C, R0, FC, R1> implements BrainNode<C, R1, FC> {
    private final BrainNode<C, R0, FC> first;
    private final BrainNode<C, R1, R0> second;

    public ChainedBrainNode(final BrainNode<C, R0, FC> first, final BrainNode<C, R1, R0> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("Chained")) {
            first.init(context, child);
            second.init(context, child);
        }
    }

    @Override
    public R1 tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
        try (final var child = logger.open("Chained")) {
            final R0 firstRes = first.tick(context, arg, child);
            child.debug("Chaining!");
            return second.tick(context, firstRes, child);
        }
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("Chained")) {
            first.deinit(context, child);
            second.deinit(context, child);
        }
    }
}
