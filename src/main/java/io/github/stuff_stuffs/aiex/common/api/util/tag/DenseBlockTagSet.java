package io.github.stuff_stuffs.aiex.common.api.util.tag;

import io.github.stuff_stuffs.aiex.common.internal.InternalBlockExtensions;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.TagKey;
import org.apache.commons.lang3.mutable.MutableObject;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DenseBlockTagSet extends DenseTagSet<Block> {
    private static final Map<TagKey<Block>, WeakReference<DenseBlockTagSet>> CACHE = new ConcurrentHashMap<>();

    private DenseBlockTagSet(final TagKey<Block> key) {
        super(key);
    }

    @Override
    protected Registry<Block> registry() {
        return Registries.BLOCK;
    }

    @Override
    protected int idFast(final Block val) {
        return ((InternalBlockExtensions) val).aiex$uniqueId();
    }

    public static DenseBlockTagSet get(final TagKey<Block> key) {
        final MutableObject<DenseBlockTagSet> result = new MutableObject<>();
        CACHE.compute(key, (k, reference) -> {
            if (reference == null) {
                final DenseBlockTagSet set = new DenseBlockTagSet(k);
                set.reset();
                result.setValue(set);
                return new WeakReference<>(set);
            }
            final DenseBlockTagSet old = reference.get();
            if (old == null) {
                final DenseBlockTagSet set = new DenseBlockTagSet(k);
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
        for (final WeakReference<DenseBlockTagSet> value : CACHE.values()) {
            final DenseBlockTagSet set = value.get();
            if (set != null) {
                set.reset();
            }
        }
    }
}
