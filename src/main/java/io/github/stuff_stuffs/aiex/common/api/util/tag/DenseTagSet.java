package io.github.stuff_stuffs.aiex.common.api.util.tag;

import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.TagKey;

import java.util.BitSet;

public abstract class DenseTagSet<T> {
    protected final TagKey<T> key;
    private final BitSet bits;

    protected DenseTagSet(final TagKey<T> key) {
        this.key = key;
        bits = new BitSet();
    }

    protected abstract Registry<T> registry();

    protected abstract int idFast(T val);

    public boolean isIn(final T ref) {
        return bits.get(idFast(ref));
    }

    public TagKey<T> key() {
        return key;
    }

    public void reset() {
        bits.clear();
        final Registry<T> registry = registry();
        for (final T ref : registry) {
            if (registry.getEntry(ref).isIn(key)) {
                bits.set(idFast(ref));
            }
        }
    }
}
