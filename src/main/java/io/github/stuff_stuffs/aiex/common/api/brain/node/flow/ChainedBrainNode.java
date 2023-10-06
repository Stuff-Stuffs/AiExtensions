package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;

public class ChainedBrainNode<C, R0, FC, R1> implements BrainNode<C, R1, FC> {
    private final BrainNode<C, R0, FC> first;
    private final BrainNode<C, R1, R0> second;

    public ChainedBrainNode(final BrainNode<C, R0, FC> first, final BrainNode<C, R1, R0> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public void init(final BrainContext<C> context) {
        first.init(context);
        second.init(context);
    }

    @Override
    public R1 tick(final BrainContext<C> context, final FC arg) {
        return second.tick(context, first.tick(context, arg));
    }

    @Override
    public void deinit(AiBrainView brain) {
        first.deinit(brain);
        second.deinit(brain);
    }
}
