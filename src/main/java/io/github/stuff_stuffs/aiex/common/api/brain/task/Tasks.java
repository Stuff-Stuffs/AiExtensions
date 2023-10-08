package io.github.stuff_stuffs.aiex.common.api.brain.task;

import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Tasks {
    public static <R, C> Task<R, C> constant(final R val) {
        return new Task<>() {
            @Override
            public R run(final BrainContext<C> context) {
                return val;
            }

            @Override
            public void stop(final AiBrainView context) {

            }
        };
    }

    public static <R, C> Task<R, C> expect(final Task<Optional<R>, C> task, final Supplier<? extends RuntimeException> errorFactory) {
        return task.adaptResult(res -> res.orElseThrow(errorFactory));
    }

    public static <R, C> Task<R, C> orElse(final Task<Optional<R>, C> task, final R fallback) {
        return task.adaptResult(res -> res.orElse(fallback));
    }

    public static <R, C> Task<R, C> orElseGet(final Task<Optional<R>, C> task, final Function<BrainContext<C>, R> factory) {
        return task.adaptResult((ctx, res) -> res.orElseGet(() -> factory.apply(ctx)));
    }

    public static <R, C> Task<Optional<R>, C> or(final Task<Optional<R>, C> task, final Function<BrainContext<C>, Optional<R>> other) {
        return task.adaptResult((ctx, res) -> res.or(() -> other.apply(ctx)));
    }

    public static <R0, R1, C> Task<R0, C> dropSecond(final Task<R0, C> first, final Task<R1, C> second) {
        return new ParallelPairTask<>(first, second, (context, r0, r1) -> r0);
    }

    public static <R0, R1, C> Task<R1, C> dropFirst(final Task<R0, C> first, final Task<R1, C> second) {
        return dropSecond(second, first);
    }

    private Tasks() {
    }
}
