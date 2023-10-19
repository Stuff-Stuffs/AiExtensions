package io.github.stuff_stuffs.aiex.common.api.util;

import io.github.stuff_stuffs.aiex.common.internal.InternalBlockExtensions;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import org.apache.commons.lang3.mutable.MutableObject;

import java.lang.ref.WeakReference;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DenseBlockTagSet {
    private static final Map<TagKey<Block>, WeakReference<DenseBlockTagSet>> CACHE = new ConcurrentHashMap<>();
    private final BitSet set = new BitSet();
    private final TagKey<Block> key;

    private DenseBlockTagSet(final TagKey<Block> key) {
        this.key = key;
    }

    public boolean isIn(final Block block) {
        return set.get(((InternalBlockExtensions) block).aiex$uniqueId());
    }

    public TagKey<Block> key() {
        return key;
    }

    private void reset() {
        set.clear();
        for (final Block block : Registries.BLOCK) {
            if (Registries.BLOCK.getEntry(block).isIn(key)) {
                set.set(((InternalBlockExtensions) block).aiex$uniqueId());
            }
        }
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
