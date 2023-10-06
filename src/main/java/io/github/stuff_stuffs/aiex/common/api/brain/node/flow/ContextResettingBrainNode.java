package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;

import java.util.function.BiPredicate;

public class ContextResettingBrainNode<C, R, FC> implements BrainNode<C, R, FC> {
    private final BrainNode<C, R, FC> node;
    private final BiPredicate<BrainContext<C>, FC> reset;

    public ContextResettingBrainNode(final BrainNode<C, R, FC> node, final BiPredicate<BrainContext<C>, FC> reset) {
        this.node = node;
        this.reset = reset;
    }

    @Override
    public void init(final BrainContext<C> context) {
        node.init(context);
    }

    @Override
    public R tick(final BrainContext<C> context, final FC arg) {
        if (reset.test(context, arg)) {
            node.deinit(context.brain());
            node.init(context);
        }
        return node.tick(context, arg);
    }

    @Override
    public void deinit(AiBrainView brain) {
        node.deinit(brain);
    }
}
