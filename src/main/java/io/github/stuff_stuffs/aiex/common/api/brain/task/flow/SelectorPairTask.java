package io.github.stuff_stuffs.aiex.common.api.brain.task.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.task.Task;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskKey;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class SelectorPairTask<R, C> implements Task<Optional<R>, C> {
    private final Function<BrainContext<C>, @Nullable Task<R, C>> trueFactory;
    private final Function<BrainContext<C>, @Nullable Task<R, C>> falseFactory;
    private final Predicate<BrainContext<C>> selector;
    private final boolean dynamic;
    private @Nullable Task<R, C> current;
    private boolean state;
    private boolean init = false;

    public <P0, P1> SelectorPairTask(final TaskKey<R, P0> trueKey, final Function<BrainContext<C>, P0> trueParameterFactory, final TaskKey<R, P1> falseKey, final Function<BrainContext<C>, P1> falseParameterFactory, final Predicate<BrainContext<C>> selector, final boolean dynamic) {
        this(context -> context.createTask(trueKey, trueParameterFactory.apply(context)).orElse(null), context -> context.createTask(falseKey, falseParameterFactory.apply(context)).orElse(null), selector, dynamic);
    }

    public SelectorPairTask(final Function<BrainContext<C>, @Nullable Task<R, C>> trueFactory, final Function<BrainContext<C>, @Nullable Task<R, C>> falseFactory, final Predicate<BrainContext<C>> selector, final boolean dynamic) {
        this.trueFactory = trueFactory;
        this.falseFactory = falseFactory;
        this.selector = selector;
        this.dynamic = dynamic;
    }

    @Override
    public Optional<R> run(final BrainContext<C> context) {
        if (!init || dynamic) {
            init = true;
            final boolean s = selector.test(context);
            if (!init || (dynamic && (state ^ s))) {
                final @Nullable Task<R, C> task;
                if (s) {
                    task = trueFactory.apply(context);
                } else {
                    task = falseFactory.apply(context);
                }
                if (current != null) {
                    current.stop(context);
                }
                current = task;
            }
            state = s;
        }
        if (current == null) {
            return Optional.empty();
        }
        return Optional.of(current.run(context));
    }

    @Override
    public void stop(final BrainContext<C> context) {
        if (current != null) {
            current.stop(context);
        }
    }
}
