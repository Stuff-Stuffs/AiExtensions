package io.github.stuff_stuffs.aiex.common.api.brain.task;

import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class SelectorPairTask<R, C, P0, P1> implements Task<Optional<R>, C> {
    private final TaskKey<R, P0> trueKey;
    private final TaskKey<R, P1> falseKey;
    private final Supplier<P0> trueParameterFactory;
    private final Supplier<P1> falseParameterFactory;
    private final Predicate<BrainContext<C>> selector;
    private final boolean dynamic;
    private @Nullable Task<R, C> current;
    private boolean state;
    private boolean init = false;

    public SelectorPairTask(final TaskKey<R, P0> trueKey, final TaskKey<R, P1> falseKey, final Supplier<P0> trueParameterFactory, final Supplier<P1> falseParameterFactory, final Predicate<BrainContext<C>> selector, final boolean dynamic) {
        this.trueKey = trueKey;
        this.falseKey = falseKey;
        this.trueParameterFactory = trueParameterFactory;
        this.falseParameterFactory = falseParameterFactory;
        this.selector = selector;
        this.dynamic = dynamic;
    }

    @Override
    public Optional<R> run(final BrainContext<C> context) {
        if (!init || dynamic) {
            init = true;
            final boolean s = selector.test(context);
            if (!init || (dynamic && (state ^ s))) {
                final Optional<Task<R, C>> task;
                if (s) {
                    task = context.createTask(trueKey, trueParameterFactory.get());
                } else {
                    task = context.createTask(falseKey, falseParameterFactory.get());
                }
                if (current != null) {
                    current.stop(context.brain());
                }
                current = task.orElse(null);
            }
            state = s;
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
