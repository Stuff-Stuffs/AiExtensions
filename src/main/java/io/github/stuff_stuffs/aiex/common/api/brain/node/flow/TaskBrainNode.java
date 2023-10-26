package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskKey;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import io.github.stuff_stuffs.aiex.common.api.util.StringTemplate;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class TaskBrainNode<C, R, P, FC, FC0> implements BrainNode<C, TaskBrainNode.Result<R>, FC> {
    private static final StringTemplate NAME_TEMPLATE = StringTemplate.create("Task-{}");
    private final TaskKey<R, P, FC0> key;
    private final BiFunction<? super FC, BrainContext<C>, ? extends P> parameterFactory;
    private final BiFunction<? super FC, BrainContext<C>, ? extends FC0> argExtractor;
    private BrainNode<C, R, FC0> task;
    private boolean error = false;

    public TaskBrainNode(final TaskKey<R, P, FC0> key, final BiFunction<? super FC, BrainContext<C>, ? extends P> parameterFactory, final BiFunction<? super FC, BrainContext<C>, ? extends FC0> argExtractor) {
        this.key = key;
        this.parameterFactory = parameterFactory;
        this.argExtractor = argExtractor;
    }


    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {
        task = null;
        error = false;
    }

    @Override
    public Result<R> tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
        try (final var child = logger.open(NAME_TEMPLATE, TaskKey.REGISTRY.getId(key).toUnderscoreSeparatedString())) {
            if (error) {
                child.debug("Returning error!");
                return new Failure<>();
            }
            if (task == null) {
                final Optional<BrainNode<C, R, FC0>> task = context.createTask(key, parameterFactory.apply(arg, context), child);
                if (task.isPresent()) {
                    this.task = task.get();
                    this.task.init(context, child);
                } else {
                    child.warning("Latching error!");
                    error = true;
                    return new Failure<>();
                }
            }
            child.debug("Entering task!");
            return new Success<>(task.tick(context, argExtractor.apply(arg, context), child));
        }
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
        if (task != null) {
            try (final var child = logger.open("Task")) {
                task.deinit(context, child);
            }
        }
        task = null;
        error = false;
    }

    public sealed interface Result<R> {
    }

    public record Success<R>(R value) implements Result<R> {
    }

    public record Failure<R>() implements Result<R> {
    }

    public static <R> Predicate<Result<R>> failurePredicate() {
        return result -> result instanceof TaskBrainNode.Failure<R>;
    }

    public static <R> Predicate<Result<R>> successPredicate() {
        return result -> result instanceof TaskBrainNode.Success<R>;
    }

    public static <R> Predicate<Result<R>> successInnerPredicate(final Predicate<R> predicate) {
        return result -> result instanceof TaskBrainNode.Success<R> success && predicate.test(success.value);
    }
}
