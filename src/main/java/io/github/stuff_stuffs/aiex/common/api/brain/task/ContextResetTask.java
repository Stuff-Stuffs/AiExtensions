package io.github.stuff_stuffs.aiex.common.api.brain.task;

import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class ContextResetTask<R, C> implements Task<Optional<R>, C> {
    private final Function<BrainContext<C>, @Nullable Task<R, C>> factory;
    private final Predicate<BrainContext<C>> reset;
    private @Nullable Task<R, C> current;

    public <P> ContextResetTask(final TaskKey<R, P> key, final Function<BrainContext<C>, P> parameterFactory, final Predicate<BrainContext<C>> reset) {
        this(context -> context.createTask(key, parameterFactory.apply(context)).orElse(null), reset);
    }

    public ContextResetTask(final Function<BrainContext<C>, @Nullable Task<R, C>> factory, final Predicate<BrainContext<C>> reset) {
        this.factory = factory;
        this.reset = reset;
    }

    @Override
    public Optional<R> run(final BrainContext<C> context) {
        if (current == null || reset.test(context)) {
            if (current != null) {
                current.stop(context.brain());
            }
            current = factory.apply(context);
        }
        if (current == null) {
            return Optional.empty();
        }
        return Optional.of(current.run(context));
    }

    @Override
    public void stop(final AiBrainView context) {
        if (current != null) {
            current.stop(context);
        }
    }
}
