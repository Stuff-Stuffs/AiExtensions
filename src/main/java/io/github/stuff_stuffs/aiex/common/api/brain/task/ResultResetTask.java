package io.github.stuff_stuffs.aiex.common.api.brain.task;

import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class ResultResetTask<R, C, P> implements Task<Optional<R>, C> {
    private final TaskKey<R, P> key;
    private final Function<BrainContext<C>, P> parameterFactory;
    private final Predicate<R> reset;
    private @Nullable Task<R, C> current = null;

    public ResultResetTask(final TaskKey<R, P> key, final Function<BrainContext<C>, P> parameterFactory, final Predicate<R> reset) {
        this.key = key;
        this.parameterFactory = parameterFactory;
        this.reset = reset;
    }

    @Override
    public Optional<R> run(final BrainContext<C> context) {
        if (current == null) {
            current = context.createTask(key, parameterFactory.apply(context)).orElse(null);
        }
        if (current == null) {
            return Optional.empty();
        }
        final R res = current.run(context);
        if (reset.test(res)) {
            current.stop(context.brain());
            current = null;
        }
        return Optional.of(res);
    }

    @Override
    public void stop(final AiBrainView context) {
        if (current != null) {
            current.stop(context);
        }
    }
}
