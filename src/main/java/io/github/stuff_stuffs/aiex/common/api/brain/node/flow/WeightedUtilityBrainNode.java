package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

public class WeightedUtilityBrainNode<C, R, FC> extends UtilityBrainNode<C, R, FC> {
    protected WeightedUtilityBrainNode(final List<Entry<C, R, FC>> entries, final boolean dynamic) {
        super(entries, dynamic);
    }

    @Override
    protected int select(final BrainContext<C> context) {
        final int size = entries.size();
        final double[] weights = new double[size];
        double sum = 0;
        for (int i = 0; i < size; i++) {
            final Entry<C, R, FC> entry = entries.get(i);
            double weight = entry.scorer.applyAsDouble(context);
            if (!(weight > 0)) {
                weight = 0;
            }
            weights[i] = weight;
            sum = sum + weight;
        }
        final Random random = new Xoroshiro128PlusPlusRandom(context.brain().randomSeed());
        final double splice = random.nextDouble() * sum;
        double runningTotal = 0;
        for (int i = 0; i < size; i++) {
            runningTotal = runningTotal + weights[i];
            if (splice < runningTotal) {
                return i;
            }
        }
        return size - 1;
    }

    public static <C, R, FC> Builder<C, R, FC> weightedBuilder() {
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

        public WeightedUtilityBrainNode<C, R, FC> build(final boolean dynamic) {
            return new WeightedUtilityBrainNode<>(List.copyOf(entries), dynamic);
        }
    }
}
