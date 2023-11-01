package io.github.stuff_stuffs.aiex.common.api.util.tag;

import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import io.github.stuff_stuffs.aiex.common.internal.InternalBlockExtensions;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.function.BooleanBiFunction;

import java.util.function.Consumer;
import java.util.function.Predicate;

class DensePredicatedBlockTagSet extends DensePredicatedTagSet<Block> {
    public DensePredicatedBlockTagSet(final DenseRefSet<Block> delegate, BooleanBiFunction combiner, final Predicate<Block> predicate, final Consumer<Runnable> resetConsumer) {
        super(delegate, combiner, predicate, resetConsumer);
    }

    @Override
    protected int maxId() {
        return AiExCommon.NEXT_BLOCK_ID.getAcquire();
    }

    @Override
    protected Registry<Block> registry() {
        return Registries.BLOCK;
    }

    @Override
    protected int idFast(final Block val) {
        return ((InternalBlockExtensions) val).aiex$uniqueId();
    }
}
