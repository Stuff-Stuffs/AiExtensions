package io.github.stuff_stuffs.aiex.common.api.brain.task;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class ContextResetTask<R, C, P> implements Task<Optional<R>, C> {
    private final TaskKey<R, P> key;
    private final Function<BrainContext<C>, P> parameterFactory;
    private final Predicate<BrainContext<C>> reset;
    private @Nullable Task<R, C> current;

    public ContextResetTask(final TaskKey<R, P> key, final Function<BrainContext<C>, P> parameterFactory, final Predicate<BrainContext<C>> reset) {
        this.key = key;
        this.parameterFactory = parameterFactory;
        this.reset = reset;
    }

    @Override
    public Optional<R> run(final BrainContext<C> context) {
        if (current == null || reset.test(context)) {
            current = context.createTask(key, parameterFactory.apply(context)).orElse(null);
        }
        if (current == null) {
            return Optional.empty();
        }
        return Optional.of(current.run(context));
    }
}
