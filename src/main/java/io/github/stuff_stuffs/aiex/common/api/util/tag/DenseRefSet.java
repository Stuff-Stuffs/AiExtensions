package io.github.stuff_stuffs.aiex.common.api.util.tag;

import net.minecraft.block.Block;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.function.BooleanBiFunction;

import java.util.List;
import java.util.function.Predicate;

public interface DenseRefSet<T> extends Predicate<T> {
    boolean isIn(T ref);

    @Override
    default boolean test(final T ref) {
        return isIn(ref);
    }

    void onReset(final Runnable runnable);

    static DenseRefSet<Block> ofBlockTag(final TagKey<Block> tag) {
        return DenseBlockRefTagSet.get(tag);
    }

    static DenseRefSet<Block> predicatedBlock(final DenseRefSet<Block> delegate, final Predicate<Block> predicate) {
        return new DensePredicatedBlockTagSet(delegate, BooleanBiFunction.AND, predicate, reset -> {
        });
    }

    static DenseRefSet<Block> and(final DenseRefSet<Block> first, final DenseRefSet<Block> second) {
        return combined(first, second, BooleanBiFunction.AND);
    }

    static DenseRefSet<Block> or(final DenseRefSet<Block> first, final DenseRefSet<Block> second) {
        return combined(first, second, BooleanBiFunction.OR);
    }

    static DenseRefSet<Block> except(final DenseRefSet<Block> delegate, final DenseRefSet<Block> except) {
        return combined(delegate, except, BooleanBiFunction.ONLY_FIRST);
    }

    static DenseRefSet<Block> combined(final DenseRefSet<Block> first, final DenseRefSet<Block> second, final BooleanBiFunction combiner) {
        return new DensePredicatedBlockTagSet(first, combiner, second::isIn, second::onReset);
    }

    static DenseRefSet<Block> all(final List<DenseRefSet<Block>> sets) {
        final int size = sets.size();
        if (size == 0) {
            throw new IllegalArgumentException();
        }
        DenseRefSet<Block> folded = sets.get(0);
        for (int i = 1; i < size; i++) {
            folded = and(folded, sets.get(i));
        }
        return folded;
    }

    static void resetAll() {
        DenseBlockRefTagSet.resetAll();
    }

    static DenseRefSet<Block> any(final List<DenseRefSet<Block>> sets) {
        final int size = sets.size();
        if (size == 0) {
            throw new IllegalArgumentException();
        }
        DenseRefSet<Block> folded = sets.get(0);
        for (int i = 1; i < size; i++) {
            folded = or(folded, sets.get(i));
        }
        return folded;
    }
}
