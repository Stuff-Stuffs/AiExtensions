package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;

import java.util.function.Predicate;

public class ResettingBrainNode<C, R, FC> implements BrainNode<C, R, FC> {
    private final BrainNode<C, R, FC> child;
    private final Predicate<R> resetPredicate;

    public ResettingBrainNode(final BrainNode<C, R, FC> child, final Predicate<R> predicate) {
        this.child = child;
        resetPredicate = predicate;
    }

    @Override
    public void init(final BrainContext<C> context) {
        child.init(context);
    }

    @Override
    public R tick(final BrainContext<C> context, final FC arg) {
        final R res = child.tick(context, arg);
        if (resetPredicate.test(res)) {
            child.deinit(context);
            child.init(context);
        }
        return res;
    }

    @Override
    public void deinit(BrainContext<C> context) {
        child.deinit(context);
    }
}
