package io.github.stuff_stuffs.aiex.common.api.brain.node;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.*;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.flow.*;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public interface BrainNode<C, R, FC> {
    void init(BrainContext<C> context, SpannedLogger logger);

    R tick(BrainContext<C> context, FC arg, SpannedLogger logger);

    void deinit(BrainContext<C> context, SpannedLogger logger);

    default BrainNode<C, Unit, FC> discardResult() {
        return adaptResult((context, r) -> Unit.INSTANCE);
    }

    default <R0> BrainNode<C, R0, FC> adaptResult(final Function<R, R0> adaptor) {
        return adaptResult((context, r) -> adaptor.apply(r));
    }

    default <R0> BrainNode<C, R0, FC> adaptResult(final BiFunction<BrainContext<C>, R, R0> adaptor) {
        return new BrainNode<>() {
            @Override
            public void init(final BrainContext<C> context, final SpannedLogger logger) {
                try (final SpannedLogger adapted = logger.open("adaptResult")) {
                    BrainNode.this.init(context, adapted);
                }
            }

            @Override
            public R0 tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
                try (final SpannedLogger adapted = logger.open("adaptResult")) {
                    return adaptor.apply(context, BrainNode.this.tick(context, arg, adapted));
                }
            }

            @Override
            public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
                try (final SpannedLogger adapted = logger.open("adaptResult")) {
                    BrainNode.this.deinit(context, adapted);
                }
            }
        };
    }

    default BrainNode<C, R, Unit> createArg(final Function<BrainContext<C>, FC> factory) {
        return adaptArg((context, unit) -> factory.apply(context));
    }

    default <FC0> BrainNode<C, R, FC0> adaptArg(final Function<FC0, FC> adaptor) {
        return adaptArg((context, arg) -> adaptor.apply(arg));
    }

    default <FC0> BrainNode<C, R, FC0> adaptArg(final BiFunction<BrainContext<C>, FC0, FC> adaptor) {
        return new BrainNode<>() {
            @Override
            public void init(final BrainContext<C> context, final SpannedLogger logger) {
                try (final SpannedLogger adapted = logger.open("adaptArg")) {
                    BrainNode.this.init(context, adapted);
                }
            }

            @Override
            public R tick(final BrainContext<C> context, final FC0 arg, final SpannedLogger logger) {
                try (final SpannedLogger adapted = logger.open("adaptArg")) {
                    return BrainNode.this.tick(context, adaptor.apply(context, arg), adapted);
                }
            }

            @Override
            public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
                try (final SpannedLogger adapted = logger.open("adaptArg")) {
                    BrainNode.this.deinit(context, adapted);
                }
            }
        };
    }

    default <R0> BrainNode<C, R0, FC> chain(final BrainNode<C, R0, R> node) {
        return new ChainedBrainNode<>(this, node);
    }

    default <FC0, R0> BrainNode<C, R0, FC> fallthroughChain(final BrainNode<C, R0, FC0> node, final BiFunction<R, FC, FC0> combiner) {
        return new FallthroughChainedBrainNode<>(this, node, combiner);
    }

    default <R0, FC0> BrainNode<C, R0, FC> ifThenFallthrough(final BiPredicate<BrainContext<C>, FC0> predicate, final BrainNode<C, R0, FC0> trueBranch, final BrainNode<C, R0, FC0> falseBranch, final BiFunction<R, FC, FC0> combiner) {
        return fallthroughChain(new IfBrainNode<>(trueBranch, falseBranch, predicate), combiner);
    }

    default BrainNode<C, R, FC> cache() {
        return cache((fc, r) -> r);
    }

    default <R0> BrainNode<C, R0, FC> cache(final BiFunction<FC, R, R0> combiner) {
        return new CachingBrainNode<>(this, combiner);
    }

    default <R0> BrainNode<C, R0, FC> ifThen(final BiPredicate<BrainContext<C>, R> predicate, final BrainNode<C, R0, R> trueBranch, final BrainNode<C, R0, R> falseBranch) {
        return chain(new IfBrainNode<>(trueBranch, falseBranch, predicate));
    }

    default BrainNode<C, R, FC> resetOnResult(final Predicate<R> predicate) {
        return new ResettingBrainNode<>(this, predicate);
    }

    default BrainNode<C, R, FC> resetOnContext(final BiPredicate<BrainContext<C>, FC> predicate) {
        return new ContextResettingBrainNode<>(this, predicate);
    }

    default BrainNode<C, Optional<R>, FC> latching(final BiPredicate<BrainContext<C>, FC> hook) {
        return new LatchingBrainNode<>(hook, this);
    }

    default <R0, R1> BrainNode<C, R1, FC> parallel(final BrainNode<C, R0, FC> node, final BiFunction<R, R0, R1> combiner) {
        return new ParallelPairBrainNode<>(this, node, (context, firstResult, secondResult) -> combiner.apply(firstResult, secondResult));
    }

    default <R0> BrainNode<C, R0, FC> contextCapture(final BiFunction<FC, R, R0> combiner) {
        return new ContextCapturingBrainNode<>(this, combiner);
    }

    default <R0, FC0> BrainNode<C, R0, FC> secondary(final BrainNode<C, R0, FC0> secondary, final BiFunction<FC, R, FC0> combiner) {
        return new SecondaryBrainNode<>(this, secondary, combiner);
    }
}
