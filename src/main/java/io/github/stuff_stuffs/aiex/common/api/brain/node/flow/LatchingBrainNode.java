package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;

import java.util.Optional;
import java.util.function.BiPredicate;

public class LatchingBrainNode<C, R, FC> implements BrainNode<C, Optional<R>, FC> {
    private final BiPredicate<BrainContext<C>, FC> hook;
    private final BrainNode<C, R, FC> delegate;
    private boolean latched = false;

    public LatchingBrainNode(final BiPredicate<BrainContext<C>, FC> hook, final BrainNode<C, R, FC> delegate) {
        this.hook = hook;
        this.delegate = delegate;
    }

    @Override
    public void init(final BrainContext<C> context) {
        latched = false;
    }

    @Override
    public Optional<R> tick(final BrainContext<C> context, final FC arg) {
        if (!latched && hook.test(context, arg)) {
            latched = true;
            delegate.init(context);
        }
        if (latched) {
            final R res = delegate.tick(context, arg);
            return Optional.of(res);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void deinit(final BrainContext<C> context) {
        if (latched) {
            delegate.deinit(context);
        }
    }
}
