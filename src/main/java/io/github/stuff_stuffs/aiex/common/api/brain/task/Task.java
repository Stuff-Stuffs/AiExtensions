package io.github.stuff_stuffs.aiex.common.api.brain.task;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Task<R, C> extends Function<BrainContext<C>, R> {
    R run(BrainContext<C> context);

    @Override
    default R apply(final BrainContext<C> context) {
        return run(context);
    }

    default <R0> Task<R0, C> adaptResult(final Function<R, R0> adaptor) {
        return context -> adaptor.apply(run(context));
    }

    default <R0> Task<R0, C> adaptResult(final BiFunction<BrainContext<C>, R, R0> adaptor) {
        return context -> adaptor.apply(context, run(context));
    }

    default Task<Optional<R>, C> whileLoop(final Predicate<R> predicate, final int max) {
        return new WhileTask<>(this, predicate, max);
    }
}
