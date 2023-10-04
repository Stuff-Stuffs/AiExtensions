package io.github.stuff_stuffs.aiex.common.api.brain.node;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.ChainedBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.FallthroughChainedBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.IfBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.ResettingBrainNode;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public interface BrainNode<C, R, FC> {
    void init(BrainContext<C> context);

    R tick(BrainContext<C> context, FC arg);

    void deinit();

    default BrainNode<C, Unit, FC> discardResult() {
        return adaptResult((context, r) -> Unit.INSTANCE);
    }

    default <R0> BrainNode<C, R0, FC> adaptResult(final Function<R, R0> adaptor) {
        return adaptResult((context, r) -> adaptor.apply(r));
    }

    default <R0> BrainNode<C, R0, FC> adaptResult(final BiFunction<BrainContext<C>, R, R0> adaptor) {
        return new BrainNode<>() {
            @Override
            public void init(final BrainContext<C> context) {
                BrainNode.this.deinit();
            }

            @Override
            public R0 tick(final BrainContext<C> context, final FC arg) {
                return adaptor.apply(context, BrainNode.this.tick(context, arg));
            }

            @Override
            public void deinit() {
                BrainNode.this.deinit();
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
            public void init(final BrainContext<C> context) {
                BrainNode.this.init(context);
            }

            @Override
            public R tick(final BrainContext<C> context, final FC0 arg) {
                return BrainNode.this.tick(context, adaptor.apply(context, arg));
            }

            @Override
            public void deinit() {
                BrainNode.this.deinit();
            }
        };
    }

    default <R0> BrainNode<C, R0, FC> chain(final BrainNode<C, R0, R> node) {
        return new ChainedBrainNode<>(this, node);
    }

    default <FC0, R0> BrainNode<C, R0, FC> fallthroughChain(final BrainNode<C, R0, FC0> node, final BiFunction<R, FC, FC0> combiner) {
        return new FallthroughChainedBrainNode<C, R, FC, FC0, R0>(this, node, combiner);
    }

    default <R0> BrainNode<C, R0, FC> ifThen(final Predicate<R> predicate, final BrainNode<C, R0, R> trueBranch, final BrainNode<C, R0, R> falseBranch) {
        return ifThen((context, r) -> predicate.test(r), trueBranch, falseBranch);
    }

    default <R0> BrainNode<C, R0, FC> ifThenTrue(final Predicate<R> predicate, final BrainNode<C, R0, R> trueBranch, final R0 failure) {
        return ifThenTrue((context, r) -> predicate.test(r), trueBranch, failure);
    }

    default <R0> BrainNode<C, R0, FC> ifThenTrue(final BiPredicate<BrainContext<C>, R> predicate, final BrainNode<C, R0, R> trueBranch, final R0 failure) {
        return ifThen(predicate, trueBranch, BrainNodes.<C, R0, Unit>constant(failure).adaptArg(arg -> Unit.INSTANCE));
    }

    default <R0> BrainNode<C, R0, FC> ifThen(final BiPredicate<BrainContext<C>, R> predicate, final BrainNode<C, R0, R> trueBranch, final BrainNode<C, R0, R> falseBranch) {
        return chain(new IfBrainNode<>(trueBranch, falseBranch, predicate));
    }

    default BrainNode<C, R, FC> resetOnResult(final Predicate<R> predicate) {
        return new ResettingBrainNode<>(this, predicate);
    }
}
