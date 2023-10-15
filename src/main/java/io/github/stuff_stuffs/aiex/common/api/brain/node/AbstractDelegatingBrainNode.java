package io.github.stuff_stuffs.aiex.common.api.brain.node;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;

import java.util.Map;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;

public abstract class AbstractDelegatingBrainNode<C, R, FC, K> implements BrainNode<C, R, FC> {
    private final ToDoubleBiFunction<BrainContext<C>, FC> threshold;
    private final R fallback;
    private K currentKey = null;
    private BrainNode<C, R, FC> child = null;
    private ToDoubleBiFunction<BrainContext<C>, FC> scorer = null;

    protected AbstractDelegatingBrainNode(final ToDoubleBiFunction<BrainContext<C>, FC> threshold, final R fallback) {
        this.threshold = threshold;
        this.fallback = fallback;
    }

    protected abstract Map<K, Entry<C, R, FC>> collectDelegates();

    @Override
    public void init(final BrainContext<C> context) {

    }

    @Override
    public R tick(final BrainContext<C> context, final FC arg) {
        if (scorer != null && scorer.applyAsDouble(context, arg) < threshold.applyAsDouble(context, arg)) {
            scorer = null;
            currentKey = null;
            child.deinit(context);
        }
        final Map<K, Entry<C, R, FC>> map = collectDelegates();
        double maxScore = Double.NaN;
        K bestKey = null;
        for (final Map.Entry<K, Entry<C, R, FC>> entry : map.entrySet()) {
            final double s = entry.getValue().scorer.applyAsDouble(context, arg);
            if (Double.isNaN(maxScore) || s > maxScore) {
                maxScore = s;
                bestKey = entry.getKey();
            }
        }
        if (bestKey == null) {
            if (child != null) {
                child.deinit(context);
                child = null;
                scorer = null;
                currentKey = null;
            }
            return fallback;
        }
        if (scorer.applyAsDouble(context, arg) < maxScore) {
            if (currentKey != null && !bestKey.equals(currentKey)) {
                currentKey = bestKey;
                child.deinit(context);
                currentKey = bestKey;
                final Entry<C, R, FC> entry = map.get(bestKey);
                scorer = entry.scorer;
                child = entry.factory.get();
                child.init(context);
            }
            if (threshold.applyAsDouble(context, arg) > maxScore) {
                child.deinit(context);
                child = null;
                scorer = null;
                currentKey = null;
            }
        }
        if (child != null) {
            return child.tick(context, arg);
        } else {
            return fallback;
        }
    }

    @Override
    public void deinit(final BrainContext<C> context) {
        if (child != null) {
            child.deinit(context);
            child = null;
            currentKey = null;
            scorer = null;
        }
    }

    protected record Entry<C, R, FC>(Supplier<? extends BrainNode<C, R, FC>> factory,
                                     ToDoubleBiFunction<BrainContext<C>, FC> scorer) {
    }
}
