package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.function.BiFunction;

public class CachingBrainNode<C, R0, R1, FC0> implements BrainNode<C, R0, FC0> {
    private final BrainNode<C, R1, FC0> cachedNode;
    private final BiFunction<FC0, R1, R0> combiner;
    private R1 cache;
    private boolean cacheInit = false;

    public CachingBrainNode(final BrainNode<C, R1, FC0> cachedNode, final BiFunction<FC0, R1, R0> combiner) {
        this.cachedNode = cachedNode;
        this.combiner = combiner;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var l = logger.open("Caching")) {
            cachedNode.init(context, l);
            cacheInit = false;
        }
    }

    @Override
    public R0 tick(final BrainContext<C> context, final FC0 arg, final SpannedLogger logger) {
        try (final var l = logger.open("Caching")) {
            if (!cacheInit) {
                l.debug("Initializing cache!");
                cache = cachedNode.tick(context, arg, l);
                cacheInit = true;
            }
            return combiner.apply(arg, cache);
        }
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var l = logger.open("Caching")) {
            cachedNode.deinit(context, l);
        }
    }
}
