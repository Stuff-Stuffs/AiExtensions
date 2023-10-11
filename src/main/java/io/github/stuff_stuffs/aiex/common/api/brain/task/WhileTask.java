package io.github.stuff_stuffs.aiex.common.api.brain.task;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;

import java.util.Optional;
import java.util.function.Predicate;

public class WhileTask<R, C> implements Task<Optional<R>, C> {
    private final Task<R, C> task;
    private final Predicate<R> resultPredicate;
    private final int attempts;

    public WhileTask(final Task<R, C> task, final Predicate<R> resultPredicate, final int attempts) {
        this.task = task;
        this.resultPredicate = resultPredicate;
        this.attempts = attempts;
    }

    @Override
    public Optional<R> run(final BrainContext<C> context) {
        for (int i = 0; i < attempts; i++) {
            final R res = task.run(context);
            if (resultPredicate.test(res)) {
                return Optional.of(res);
            }
        }
        return Optional.empty();
    }

    @Override
    public void stop(final BrainContext<C> context) {
        task.stop(context);
    }
}
