package io.github.stuff_stuffs.aiex.common.api.util.tag;

import io.github.stuff_stuffs.aiex.common.internal.InternalBlockExtensions;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.TagKey;
import org.apache.commons.lang3.mutable.MutableObject;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CombinedDenseBlockTagSet extends CombinedDenseTagSet<Block> {
    private static final Map<Set<TagKey<Block>>, WeakReference<CombinedDenseBlockTagSet>> CACHE = new ConcurrentHashMap<>();

    private CombinedDenseBlockTagSet(final Set<TagKey<Block>> set) {
        super(set);
    }

    @Override
    protected Registry<Block> registry() {
        return Registries.BLOCK;
    }

    @Override
    protected int idFast(final Block val) {
        return ((InternalBlockExtensions) val).aiex$uniqueId();
    }

    public static CombinedDenseBlockTagSet get(Set<TagKey<Block>> key) {
        key = Set.copyOf(key);
        final MutableObject<CombinedDenseBlockTagSet> result = new MutableObject<>();
        CACHE.compute(key, (k, reference) -> {
            if (reference == null) {
                final CombinedDenseBlockTagSet set = new CombinedDenseBlockTagSet(k);
                set.reset();
                result.setValue(set);
                return new WeakReference<>(set);
            }
            final CombinedDenseBlockTagSet old = reference.get();
            if (old == null) {
                final CombinedDenseBlockTagSet set = new CombinedDenseBlockTagSet(k);
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
        for (final WeakReference<CombinedDenseBlockTagSet> value : CACHE.values()) {
            final CombinedDenseBlockTagSet set = value.get();
            if (set != null) {
                set.reset();
            }
        }
    }
}
