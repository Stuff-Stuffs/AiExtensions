package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;

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
    public void init(final BrainContext<C> context) {
        first.init(context);
        second.init(context);
    }

    @Override
    public R1 tick(final BrainContext<C> context, final FC0 arg) {
        return second.tick(context, combiner.apply(first.tick(context, arg), arg));
    }

    @Override
    public void deinit(BrainContext<C> context) {
        first.deinit(context);
        second.deinit(context);
    }
}
