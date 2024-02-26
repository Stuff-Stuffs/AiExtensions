package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import com.mojang.datafixers.util.Pair;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class LoopBrainNode<C, R, FC, A> implements BrainNode<C, R, FC> {
    private final BrainNode<C, A, A> delegate;
    private final BiPredicate<BrainContext<C>, A> stopPredicate;
    private final BiFunction<BrainContext<C>, FC, A> start;
    private final BiFunction<BrainContext<C>, A, R> finish;
    private final int maxIterations;

    public LoopBrainNode(final BrainNode<C, A, A> delegate, final BiPredicate<BrainContext<C>, A> stopPredicate, final BiFunction<BrainContext<C>, FC, A> start, final BiFunction<BrainContext<C>, A, R> finish, final int maxIterations) {
        this.delegate = delegate;
        this.stopPredicate = stopPredicate;
        this.start = start;
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
                acc = delegate.tick(context, acc, child);
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

    public static <C, R, E, A> BrainNode<C, R, Iterable<E>> forEach(final BrainNode<C, A, Pair<A, E>> delegate, final BiPredicate<BrainContext<C>, A> stopPredicate, final Function<BrainContext<C>, A> start, final BiFunction<BrainContext<C>, A, R> finish, final int maxIterations) {
        return new LoopBrainNode<>(
                new BrainNode<>() {
                    @Override
                    public void init(final BrainContext<C> context, final SpannedLogger logger) {
                        delegate.init(context, logger);
                    }

                    @Override
                    public ForEachState<E, A> tick(final BrainContext<C> context, final ForEachState<E, A> arg, final SpannedLogger logger) {
                        arg.state = delegate.tick(context, Pair.of(arg.state, arg.iterator.next()), logger);
                        return arg;
                    }

                    @Override
                    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
                        delegate.deinit(context, logger);
                    }
                },
                (context, state) -> !state.iterator.hasNext() || stopPredicate.test(context, state.state),
                (context, es) -> new ForEachState<>(es.iterator(), start.apply(context)),
                (context, state) -> finish.apply(context, state.state),
                maxIterations
        );
    }

    public static <C, E> BrainNode<C, Optional<E>, Iterable<E>> first(final BrainNode<C, Boolean, E> stopPredicate, final int maxIterations) {
        return LoopBrainNode.<C, Optional<E>, E, Optional<E>>forEach(
                new BrainNode<>() {
                    @Override
                    public void init(final BrainContext<C> context, final SpannedLogger logger) {
                        stopPredicate.init(context, logger);
                    }

                    @Override
                    public Optional<E> tick(final BrainContext<C> context, final Pair<Optional<E>, E> arg, final SpannedLogger logger) {
                        return stopPredicate.tick(context, arg.getSecond(), logger) ? Optional.of(arg.getSecond()) : Optional.empty();
                    }

                    @Override
                    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
                        stopPredicate.deinit(context, logger);
                    }
                },
                (context, r) -> r.isPresent(),
                context -> Optional.empty(),
                (context, r) -> r,
                maxIterations
        );
    }

    private static final class ForEachState<E, A> {
        private final Iterator<E> iterator;
        private A state;

        private ForEachState(final Iterator<E> iterator, final A state) {
            this.iterator = iterator;
            this.state = state;
        }
    }
}
