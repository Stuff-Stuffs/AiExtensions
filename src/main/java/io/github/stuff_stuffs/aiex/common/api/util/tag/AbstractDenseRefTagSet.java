package io.github.stuff_stuffs.aiex.common.api.util.tag;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.TagKey;

import java.lang.ref.WeakReference;
import java.util.Set;

public abstract class AbstractDenseRefTagSet<T> implements DenseRefSet<T> {
    static final int SHIFT = Integer.SIZE - Integer.numberOfLeadingZeros(Long.SIZE);
    static final int MASK = (1 << SHIFT) - 1;
    private final Set<WeakReference<Runnable>> onReset = new ObjectOpenHashSet<>();
    protected final TagKey<T> key;
    private long[] bits;

    protected AbstractDenseRefTagSet(final TagKey<T> key) {
        this.key = key;
        bits = new long[0];
    }

    @Override
    public void onReset(final Runnable runnable) {
        onReset.add(new WeakReference<>(runnable));
    }

    protected abstract Registry<T> registry();

    protected abstract int idFast(T val);

    @Override
    public boolean isIn(final T ref) {
        final int bit = idFast(ref);
        return (bits[bit >>> SHIFT] >>> (bit & MASK) & 1) == 1;
    }

    public TagKey<T> key() {
        return key;
    }

    public void reset() {
        final Registry<T> registry = registry();
        bits = new long[(registry.size() + Long.SIZE - 1) / Long.SIZE];
        for (final T ref : registry) {
            if (registry.getEntry(ref).isIn(key)) {
                final int bit = idFast(ref);
                bits[bit >>> SHIFT] |= 1L << (bit & MASK);
            }
        }
        onReset.removeIf(p -> p.get() == null);
        for (final WeakReference<Runnable> reference : onReset) {
            final Runnable runnable = reference.get();
            if (runnable != null) {
                runnable.run();
            }
        }
    }
}
