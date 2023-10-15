package io.github.stuff_stuffs.aiex.common.api.brain.util.target;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;

import java.util.Optional;

public abstract class AbstractCachingTargeter<C, R, FC> implements Targeter<C, Optional<R>, FC> {
    private final boolean dynamic;
    private Optional<R> cached = Optional.empty();
    private boolean cacheInit = false;

    protected AbstractCachingTargeter(final boolean dynamic) {
        this.dynamic = dynamic;
    }

    protected abstract Optional<R> query(BrainContext<C> context, FC arg);

    @Override
    public Optional<R> find(final BrainContext<C> context, final FC arg) {
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
    public void reset() {
        cached = Optional.empty();
        cacheInit = false;
    }
}
