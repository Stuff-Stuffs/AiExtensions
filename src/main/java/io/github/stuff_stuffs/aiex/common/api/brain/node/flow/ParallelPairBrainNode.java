package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

public class ParallelPairBrainNode<C, R0, R1, R2, FC> implements BrainNode<C, R0, FC> {
    private final BrainNode<C, R1, FC> first;
    private final BrainNode<C, R2, FC> second;
    private final ResultMerger<C, R0, R1, R2> merger;

    public ParallelPairBrainNode(final BrainNode<C, R1, FC> first, final BrainNode<C, R2, FC> second, final ResultMerger<C, R0, R1, R2> merger) {
        this.first = first;
        this.second = second;
        this.merger = merger;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("ParallelPair")) {
            first.init(context, child);
            second.init(context, child);
        }
    }

    @Override
    public R0 tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
        try (final var child = logger.open("ParallelPair")) {
            final R1 firstRes = first.tick(context, arg, child);
            final R2 secondRes = second.tick(context, arg, child);
            return merger.merge(context, firstRes, secondRes);
        }
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("ParallelPair")) {
            first.deinit(context, child);
            second.deinit(context, child);
        }
    }

    public interface ResultMerger<C, R0, R1, R2> {
        R0 merge(BrainContext<C> context, R1 firstResult, R2 secondResult);
    }
}
