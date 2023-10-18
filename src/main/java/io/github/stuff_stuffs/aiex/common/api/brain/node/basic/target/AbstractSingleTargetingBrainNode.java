package io.github.stuff_stuffs.aiex.common.api.brain.node.basic.target;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.Optional;

public abstract class AbstractSingleTargetingBrainNode<C, R, FC> implements BrainNode<C, Optional<R>, FC> {
    private final boolean dynamic;
    private Optional<R> cached = Optional.empty();
    private boolean cacheInit = false;

    protected AbstractSingleTargetingBrainNode(final boolean dynamic) {
        this.dynamic = dynamic;
    }

    @Override
    public void init(final BrainContext<C> context, SpannedLogger logger) {
        cached = Optional.empty();
        cacheInit = false;
    }

    protected abstract Optional<R> query(BrainContext<C> context, FC arg);

    @Override
    public Optional<R> tick(final BrainContext<C> context, final FC arg, SpannedLogger logger) {
        final Optional<R> opt;
        if (dynamic || !cacheInit) {
            opt = query(context, arg);
            cached = opt;
            cacheInit = true;
        } else {
            opt = cached;
        }
        return opt;
    }

    @Override
    public void deinit(BrainContext<C> context, SpannedLogger logger) {
        cached = Optional.empty();
        cacheInit = false;
    }
}
