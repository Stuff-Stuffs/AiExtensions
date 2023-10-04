package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

public class UtilityBrainNode<C, R, FC> implements BrainNode<C, R, FC> {
    protected final List<Entry<C, R, FC>> entries;
    protected final boolean dynamic;
    private int selected = -1;

    protected UtilityBrainNode(final List<Entry<C, R, FC>> entries, final boolean dynamic) {
        this.entries = entries;
        this.dynamic = dynamic;
    }

    protected int select(final BrainContext<C> context) {
        double bestScore = Double.NEGATIVE_INFINITY;
        int bestIndex = -1;
        final int size = entries.size();
        for (int i = 0; i < size; i++) {
            final Entry<C, R, FC> entry = entries.get(i);
            final double score = entry.scorer.applyAsDouble(context);
            if (score > bestScore) {
                bestIndex = i;
                bestScore = score;
            }
        }
        return bestIndex;
    }

    @Override
    public void init(final BrainContext<C> context) {
        selected = -1;
        if (dynamic) {
            return;
        }
        selected = select(context);
    }

    @Override
    public R tick(final BrainContext<C> context, final FC arg) {
        if (dynamic) {
            final int selected = select(context);
            if (selected != this.selected) {
                if (this.selected != -1) {
                    entries.get(this.selected).node.deinit();
                }
                entries.get(selected).node.init(context);
            }
            this.selected = selected;
        }
        return entries.get(selected).node.tick(context, arg);
    }

    @Override
    public void deinit() {
        if (selected != -1) {
            entries.get(selected).node.deinit();
            selected = -1;
        }
    }

    public static <C, R, FC> Builder<C, R, FC> builder() {
        return new Builder<>();
    }

    public static final class Builder<C, R, FC> {
        private final List<Entry<C, R, FC>> entries;

        private Builder() {
            entries = new ArrayList<>();
        }

        public Builder<C, R, FC> add(final BrainNode<C, R, FC> node, final ToDoubleFunction<BrainContext<C>> scorer) {
            entries.add(new Entry<>(node, scorer));
            return this;
        }

        public UtilityBrainNode<C, R, FC> build(final boolean dynamic) {
            return new UtilityBrainNode<>(List.copyOf(entries), dynamic);
        }
    }

    protected static class Entry<C, R, FC> {
        protected final BrainNode<C, R, FC> node;
        protected final ToDoubleFunction<BrainContext<C>> scorer;

        protected Entry(final BrainNode<C, R, FC> node, final ToDoubleFunction<BrainContext<C>> scorer) {
            this.node = node;
            this.scorer = scorer;
        }

        @Override
        public String toString() {
            return "Entry[" +
                    "node=" + node + ", " +
                    "scorer=" + scorer + ']';
        }
    }
}
