package io.github.stuff_stuffs.aiex.common.api.util;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public final class AiExFunctionUtil {
    public static <C> Predicate<C> eagerOr(final Predicate<C> first, final Predicate<C> second) {
        return context -> first.test(context) | second.test(context);
    }

    public static <C> Predicate<C> eagerAnd(final Predicate<C> first, final Predicate<C> second) {
        return context -> first.test(context) & second.test(context);
    }

    public static <C0, C1> BiPredicate<C0, C1> eagerOr(final BiPredicate<C0, C1> first, final BiPredicate<C0, C1> second) {
        return (f, s) -> first.test(f, s) | second.test(f, s);
    }

    public static <C0, C1> BiPredicate<C0, C1> eagerAnd(final BiPredicate<C0, C1> first, final BiPredicate<C0, C1> second) {
        return (f, s) -> first.test(f, s) & second.test(f, s);
    }

    public static <C, V> Predicate<C> watcher(final Function<C, V> extractor, final boolean detectFirst) {
        return new Predicate<>() {
            private V last = null;
            private boolean init = false;

            @Override
            public boolean test(final C context) {
                if (!init) {
                    init = true;
                    last = extractor.apply(context);
                    return detectFirst;
                }
                final V current = extractor.apply(context);
                if (current.equals(last)) {
                    return false;
                }
                last = current;
                return true;
            }
        };
    }

    public static <C0, C1, V> BiPredicate<C0, C1> watcher(final BiFunction<C0, C1, V> extractor, final boolean detectFirst) {
        return new BiPredicate<>() {
            private V last = null;
            private boolean init = false;

            @Override
            public boolean test(final C0 first, final C1 second) {
                if (!init) {
                    init = true;
                    last = extractor.apply(first, second);
                    return detectFirst;
                }
                final V current = extractor.apply(first, second);
                if (current.equals(last)) {
                    return false;
                }
                last = current;
                return true;
            }
        };
    }

    private AiExFunctionUtil() {
    }
}
