package io.github.stuff_stuffs.aiex.common.api.brain.task.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

public class ForwardingBrainNode<C, R> implements BrainNode<C, R, R> {
    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {

    }

    @Override
    public R tick(final BrainContext<C> context, final R arg, final SpannedLogger logger) {
        return arg;
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {

    }
}
