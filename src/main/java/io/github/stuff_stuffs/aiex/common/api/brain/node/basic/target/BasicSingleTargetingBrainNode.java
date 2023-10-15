package io.github.stuff_stuffs.aiex.common.api.brain.node.basic.target;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.util.target.Targeter;

public class BasicSingleTargetingBrainNode<C, R, FC> implements BrainNode<C, R, FC> {
    private final Targeter<C, R, FC> targeter;

    public BasicSingleTargetingBrainNode(final Targeter<C, R, FC> targeter) {
        this.targeter = targeter;
    }

    @Override
    public void init(final BrainContext<C> context) {
        targeter.reset();
    }

    @Override
    public R tick(final BrainContext<C> context, final FC arg) {
        return targeter.find(context, arg);
    }

    @Override
    public void deinit(final BrainContext<C> context) {
        targeter.reset();
    }
}
