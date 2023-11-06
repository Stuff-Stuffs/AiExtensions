package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class LoopBrainNode<C, R, R0, FC, A> implements BrainNode<C, R, FC> {
    private final BrainNode<C, R0, FC> delegate;
    private final BiPredicate<BrainContext<C>, A> stopPredicate;
    private final BiFunction<BrainContext<C>, FC, A> start;
    private final BiFunction<A, R0, A> fold;
    private final BiFunction<BrainContext<C>, A, R> finish;
    private final int maxIterations;

    public LoopBrainNode(final BrainNode<C, R0, FC> delegate, final BiPredicate<BrainContext<C>, A> stopPredicate, final BiFunction<BrainContext<C>, FC, A> start, final BiFunction<A, R0, A> fold, final BiFunction<BrainContext<C>, A, R> finish, final int maxIterations) {
        this.delegate = delegate;
        this.stopPredicate = stopPredicate;
        this.start = start;
        this.fold = fold;
        this.finish = finish;
        this.maxIterations = maxIterations;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("Loop")) {
            delegate.init(context, child);
        }
    }

    @Override
    public R tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
        try (final var child = logger.open("Loop")) {
            A acc = start.apply(context, arg);
            for (int i = 0; i < maxIterations; i++) {
                if (stopPredicate.test(context, acc)) {
                    return finish.apply(context, acc);
                }
                acc = fold.apply(acc, delegate.tick(context, arg, child));
            }
            return finish.apply(context, acc);
        }
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("Loop")) {
            delegate.deinit(context, child);
        }
    }
}
