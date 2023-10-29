package io.github.stuff_stuffs.aiex.common.api.util.tag;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.registry.Registry;
import net.minecraft.util.function.BooleanBiFunction;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class DensePredicatedTagSet<T> implements DenseRefSet<T> {
    private final Set<WeakReference<Runnable>> onReset = new ObjectOpenHashSet<>();
    private long[] bits;
    private final Runnable reset;

    public DensePredicatedTagSet(final DenseRefSet<T> delegate, final BooleanBiFunction combiner, final Predicate<T> predicate, final Consumer<Runnable> resetConsumer) {
        bits = new long[0];
        reset = () -> {
            final Registry<T> registry = registry();
            bits = new long[(registry.size() + Long.SIZE - 1) / Long.SIZE];
            for (final T ref : registry) {
                if (combiner.apply(delegate.isIn(ref), predicate.test(ref))) {
                    final int bit = idFast(ref);
                    bits[bit >>> AbstractDenseRefTagSet.SHIFT] |= 1L << (bit & AbstractDenseRefTagSet.MASK);
                }
            }
            onReset.removeIf(ref -> ref.get() == null);
            for (final WeakReference<Runnable> reference : onReset) {
                final Runnable runnable = reference.get();
                if (runnable != null) {
                    runnable.run();
                }
            }
        };
        reset.run();
        resetConsumer.accept(reset);
        delegate.onReset(reset);
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
        return (bits[bit >>> AbstractDenseRefTagSet.SHIFT] >>> (bit & AbstractDenseRefTagSet.MASK) & 1) == 1;
    }
}
