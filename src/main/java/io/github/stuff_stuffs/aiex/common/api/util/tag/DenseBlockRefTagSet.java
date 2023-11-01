package io.github.stuff_stuffs.aiex.common.api.util.tag;

import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import io.github.stuff_stuffs.aiex.common.internal.InternalBlockExtensions;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.TagKey;
import org.apache.commons.lang3.mutable.MutableObject;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class DenseBlockRefTagSet extends AbstractDenseRefTagSet<Block> {
    private static final Map<TagKey<Block>, WeakReference<DenseBlockRefTagSet>> CACHE = new ConcurrentHashMap<>();

    private DenseBlockRefTagSet(final TagKey<Block> key) {
        super(key);
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

    public static DenseBlockRefTagSet get(final TagKey<Block> key) {
        final MutableObject<DenseBlockRefTagSet> result = new MutableObject<>();
        CACHE.compute(key, (k, reference) -> {
            if (reference == null) {
                final DenseBlockRefTagSet set = new DenseBlockRefTagSet(k);
                set.reset();
                result.setValue(set);
                return new WeakReference<>(set);
            }
            final DenseBlockRefTagSet old = reference.get();
            if (old == null) {
                final DenseBlockRefTagSet set = new DenseBlockRefTagSet(k);
                set.reset();
                result.setValue(set);
                return new WeakReference<>(set);
            }
            result.setValue(old);
            return reference;
        });
        return result.getValue();
    }

    public static void resetAll() {
        for (final WeakReference<DenseBlockRefTagSet> value : CACHE.values()) {
            final DenseBlockRefTagSet set = value.get();
            if (set != null) {
                set.reset();
            }
        }
    }
}
