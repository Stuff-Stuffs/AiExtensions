package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.function.BiFunction;

public class SecondaryBrainNode<C, R, FC0, FC1, FC2> implements BrainNode<C, R, FC0> {
    private final BrainNode<C, FC1, FC0> argCreator;
    private final BrainNode<C, R, FC2> main;
    private final BiFunction<FC0, FC1, FC2> combiner;

    public SecondaryBrainNode(final BrainNode<C, FC1, FC0> creator, final BrainNode<C, R, FC2> main, final BiFunction<FC0, FC1, FC2> combiner) {
        argCreator = creator;
        this.main = main;
        this.combiner = combiner;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("Secondary")) {
            argCreator.init(context, child);
            main.init(context, child);
        }
    }

    @Override
    public R tick(final BrainContext<C> context, final FC0 arg, final SpannedLogger logger) {
        try (final var child = logger.open("Secondary")) {
            return main.tick(context, combiner.apply(arg, argCreator.tick(context, arg, child)), child);
        }
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = logger.open("Secondary")) {
            argCreator.deinit(context, child);
            main.deinit(context, child);
        }
    }
}
