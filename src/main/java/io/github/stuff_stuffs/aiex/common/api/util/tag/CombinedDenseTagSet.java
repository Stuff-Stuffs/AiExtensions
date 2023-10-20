package io.github.stuff_stuffs.aiex.common.api.util.tag;

import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.TagKey;

import java.util.BitSet;
import java.util.Set;

public abstract class CombinedDenseTagSet<T> {
    private final Set<TagKey<T>> set;
    private final BitSet any;
    private final BitSet all;

    protected CombinedDenseTagSet(final Set<TagKey<T>> set) {
        this.set = set;
        any = new BitSet();
        all = new BitSet();
    }

    protected abstract Registry<T> registry();

    protected abstract int idFast(T val);

    public Set<TagKey<T>> keySet() {
        return set;
    }

    public boolean isInAny(final T ref) {
        return any.get(idFast(ref));
    }

    public boolean isInAll(final T ref) {
        return all.get(idFast(ref));
    }

    public void reset() {
        any.clear();
        all.clear();
        final Registry<T> registry = registry();
        for (final T ref : registry) {
            boolean any = false;
            boolean all = true;
            for (final TagKey<T> key : set) {
                if (registry.getEntry(ref).isIn(key)) {
                    any = true;
                } else {
                    all = false;
                }
                if (any & !all) {
                    break;
                }
            }
            if (any) {
                this.any.set(idFast(ref));
            }
            if (all) {
                this.all.set(idFast(ref));
            }
        }
    }
}
