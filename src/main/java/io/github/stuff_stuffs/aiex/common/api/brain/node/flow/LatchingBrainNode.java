package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class LatchingBrainNode<C, R, FC> implements BrainNode<C, Optional<R>, FC> {
    private final Predicate<BrainContext<C>> hook;
    private final BiPredicate<BrainContext<C>, R> unhook;
    private final BrainNode<C, R, FC> delegate;
    private boolean latched = false;

    public LatchingBrainNode(final Predicate<BrainContext<C>> hook, final BiPredicate<BrainContext<C>, R> unhook, final BrainNode<C, R, FC> delegate) {
        this.hook = hook;
        this.unhook = unhook;
        this.delegate = delegate;
    }

    @Override
    public void init(final BrainContext<C> context) {
        delegate.init(context);
        latched = false;
    }

    @Override
    public Optional<R> tick(final BrainContext<C> context, final FC arg) {
        if (!latched && hook.test(context)) {
            latched = true;
        }
        if (latched) {
            final R res = delegate.tick(context, arg);
            if (unhook.test(context, res)) {
                latched = false;
            }
            return Optional.of(res);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void deinit(BrainContext<C> context) {
        delegate.deinit(context);
    }
}
