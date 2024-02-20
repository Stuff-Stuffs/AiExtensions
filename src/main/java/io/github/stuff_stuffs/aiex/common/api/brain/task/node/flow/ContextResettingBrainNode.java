package io.github.stuff_stuffs.aiex.common.api.brain.task.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.function.BiPredicate;

public class ContextResettingBrainNode<C, R, FC> implements BrainNode<C, R, FC> {
    private final BrainNode<C, R, FC> node;
    private final BiPredicate<BrainContext<C>, FC> reset;

    public ContextResettingBrainNode(final BrainNode<C, R, FC> node, final BiPredicate<BrainContext<C>, FC> reset) {
        this.node = node;
        this.reset = reset;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("ContextReset")) {
            node.init(context, child);
        }
    }

    @Override
    public R tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
        try (final var child = logger.open("ContextReset")) {
            if (reset.test(context, arg)) {
                child.debug("Resetting");
                node.deinit(context, child);
                node.init(context, child);
            }
            return node.tick(context, arg, child);
        }
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("ContextReset")) {
            node.deinit(context, child);
        }
    }
}
