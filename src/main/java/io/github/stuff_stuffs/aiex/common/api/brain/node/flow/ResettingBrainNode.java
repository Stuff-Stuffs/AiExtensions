package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.function.Predicate;

public class ResettingBrainNode<C, R, FC> implements BrainNode<C, R, FC> {
    private final BrainNode<C, R, FC> child;
    private final Predicate<R> resetPredicate;

    public ResettingBrainNode(final BrainNode<C, R, FC> child, final Predicate<R> predicate) {
        this.child = child;
        resetPredicate = predicate;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("ResultResetting")) {
            this.child.init(context, child);
        }
    }

    @Override
    public R tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
        try (final var child = logger.open("ResultResetting")) {
            final R res = this.child.tick(context, arg, child);
            if (resetPredicate.test(res)) {
                child.debug("resetting!");
                this.child.deinit(context, child);
                this.child.init(context, child);
            }
            return res;
        }
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("ResultResetting")) {
            this.child.deinit(context, child);
        }
    }
}
