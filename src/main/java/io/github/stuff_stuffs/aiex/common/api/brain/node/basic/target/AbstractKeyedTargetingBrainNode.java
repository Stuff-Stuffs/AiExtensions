package io.github.stuff_stuffs.aiex.common.api.brain.node.basic.target;

import com.mojang.datafixers.util.Pair;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractKeyedTargetingBrainNode<C, R, FC, K> implements BrainNode<C, R, FC> {
    private @Nullable K key;
    private @Nullable R cached;

    @Override
    public void init(final BrainContext<C> context, SpannedLogger logger) {

    }

    protected abstract boolean shouldInvalidate(BrainContext<C> context, FC arg, K oldKey);

    protected abstract Pair<K, R> query(BrainContext<C> context, FC arg);

    @Override
    public R tick(final BrainContext<C> context, final FC arg, SpannedLogger logger) {
        if (key == null || shouldInvalidate(context, arg, key)) {
            final Pair<K, R> query = query(context, arg);
            key = query.getFirst();
            cached = query.getSecond();
        }
        return cached;
    }

    @Override
    public void deinit(final BrainContext<C> context, SpannedLogger logger) {
        key = null;
        cached = null;
    }
}
