package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.function.BiFunction;

public class CachingChainedBrainNode<C, R0, R1, FC0, FC1> implements BrainNode<C, R0, FC0> {
    private final BrainNode<C, R1, FC0> cachedNode;
    private final BrainNode<C, R0, FC1> then;
    private final BiFunction<FC0, R1, FC1> combiner;
    private R1 cache;
    private boolean cacheInit = false;

    public CachingChainedBrainNode(final BrainNode<C, R1, FC0> cachedNode, final BrainNode<C, R0, FC1> then, final BiFunction<FC0, R1, FC1> combiner) {
        this.cachedNode = cachedNode;
        this.then = then;
        this.combiner = combiner;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var l = logger.open("CachingChained")) {
            cachedNode.init(context, l);
            then.init(context, l);
            cacheInit = false;
        }
    }

    @Override
    public R0 tick(final BrainContext<C> context, final FC0 arg, final SpannedLogger logger) {
        try (final var l = logger.open("CachingChained")) {
            if (!cacheInit) {
                l.debug("Initializing cache!");
                cache = cachedNode.tick(context, arg, l);
                cacheInit = true;
            }
            return then.tick(context, combiner.apply(arg, cache), l);
        }
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var l = logger.open("CachingChained")) {
            cachedNode.deinit(context, l);
            then.deinit(context, l);
        }
    }
}
