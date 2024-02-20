package io.github.stuff_stuffs.aiex.common.api.brain.task.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.Optional;
import java.util.function.BiPredicate;

public class LatchingBrainNode<C, R, FC> implements BrainNode<C, Optional<R>, FC> {
    private final BiPredicate<BrainContext<C>, FC> hook;
    private final BrainNode<C, R, FC> delegate;
    private boolean latched = false;

    public LatchingBrainNode(final BiPredicate<BrainContext<C>, FC> hook, final BrainNode<C, R, FC> delegate) {
        this.hook = hook;
        this.delegate = delegate;
    }

    @Override
    public void init(final BrainContext<C> context, SpannedLogger logger) {
        try (final var child = logger.open("latch")) {
            latched = false;
        }
    }

    @Override
    public Optional<R> tick(final BrainContext<C> context, final FC arg, SpannedLogger logger) {
        try (final var child = logger.open("latch")) {
            if (!latched && hook.test(context, arg)) {
                latched = true;
                child.debug("Latching!");
                delegate.init(context, child);
            }
            if (latched) {
                final R res = delegate.tick(context, arg, child);
                return Optional.of(res);
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public void deinit(final BrainContext<C> context, SpannedLogger logger) {
        if (latched) {
            try (final var child = logger.open("latch")) {
                delegate.deinit(context, child);
            }
            latched = false;
        }
    }
}
