package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;

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
    public void init(final BrainContext<C> context) {
        first.init(context);
        second.init(context);
    }

    @Override
    public R0 tick(final BrainContext<C> context, final FC arg) {
        return merger.merge(context, first.tick(context, arg), second.tick(context, arg));
    }

    @Override
    public void deinit() {
        first.deinit();
        second.deinit();
    }

    public interface ResultMerger<C, R0, R1, R2> {
        R0 merge(BrainContext<C> context, R1 firstResult, R2 secondResult);
    }
}
