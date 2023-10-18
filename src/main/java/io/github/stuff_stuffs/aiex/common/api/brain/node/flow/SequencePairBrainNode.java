package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public class SequencePairBrainNode<C, R0, R1, R2, FC> implements BrainNode<C, R0, FC> {
    private final BrainNode<C, R1, FC> first;
    private final BrainNode<C, R2, FC> second;
    private final Predicate<R1> transitionPredicate;
    private final BiFunction<BrainContext<C>, R1, R0> firstAdaptor;
    private final BiFunction<BrainContext<C>, R2, R0> secondAdaptor;
    private boolean state = false;

    public SequencePairBrainNode(final BrainNode<C, R1, FC> first, final BrainNode<C, R2, FC> second, final Predicate<R1> transitionPredicate, final BiFunction<BrainContext<C>, R1, R0> firstAdaptor, final BiFunction<BrainContext<C>, R2, R0> secondAdaptor) {
        this.first = first;
        this.second = second;
        this.transitionPredicate = transitionPredicate;
        this.firstAdaptor = firstAdaptor;
        this.secondAdaptor = secondAdaptor;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("SequencedPair")) {
            first.init(context, child);
            second.init(context, child);
        }
        state = false;
    }

    @Override
    public R0 tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
        try (final var child = logger.open("SequencedPair")) {
            if (!state) {
                child.debug("Entering First!");
                final R1 res = first.tick(context, arg, child);
                if (transitionPredicate.test(res)) {
                    child.debug("Transitioning!");
                    state = true;
                }
                return firstAdaptor.apply(context, res);
            } else {
                child.debug("Entering Second!");
                return secondAdaptor.apply(context, second.tick(context, arg, child));
            }
        }
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("SequencedPair")) {
            first.deinit(context, child);
            second.deinit(context, child);
        }
        state = false;
    }
}
