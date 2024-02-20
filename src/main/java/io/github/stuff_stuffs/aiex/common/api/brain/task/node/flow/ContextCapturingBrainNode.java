package io.github.stuff_stuffs.aiex.common.api.brain.task.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.function.BiFunction;

public class ContextCapturingBrainNode<C, R, R0, FC> implements BrainNode<C, R, FC> {
    private final BrainNode<C, R0, FC> delegate;
    private final BiFunction<FC, R0, R> combiner;

    public ContextCapturingBrainNode(final BrainNode<C, R0, FC> delegate, final BiFunction<FC, R0, R> combiner) {
        this.delegate = delegate;
        this.combiner = combiner;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("ContextCapture")) {
            delegate.init(context, child);
        }
    }

    @Override
    public R tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
        try (final var child = logger.open("ContextCapture")) {
            return combiner.apply(arg, delegate.tick(context, arg, child));
        }
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("ContextCapture")) {
            delegate.deinit(context, child);
        }
    }
}
