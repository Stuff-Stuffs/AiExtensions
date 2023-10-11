package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.Task;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskKey;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class TaskTerminalBrainNode<C, R, P, FC> implements BrainNode<C, TaskTerminalBrainNode.Result<R>, FC> {
    private final TaskKey<R, P> key;
    private final BiFunction<? super FC, BrainContext<C>, ? extends P> parameterFactory;
    private Task<R, C> task;
    private boolean error = false;

    public TaskTerminalBrainNode(final TaskKey<R, P> key, final BiFunction<? super FC, BrainContext<C>, ? extends P> parameterFactory) {
        this.key = key;
        this.parameterFactory = parameterFactory;
    }

    @Override
    public void init(final BrainContext<C> context) {
        task = null;
        error = false;
    }

    @Override
    public Result<R> tick(final BrainContext<C> context, final FC arg) {
        if (error) {
            return new Failure<>();
        }
        if (task == null) {
            final Optional<Task<R, C>> task = context.createTask(key, parameterFactory.apply(arg, context));
            if (task.isPresent()) {
                this.task = task.get();
            } else {
                error = true;
                return new Failure<>();
            }
        }
        return new Success<>(task.run(context));
    }

    @Override
    public void deinit(final BrainContext<C> context) {
        if (task != null) {
            task.stop(context);
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
        return result -> result instanceof TaskTerminalBrainNode.Failure<R>;
    }

    public static <R> Predicate<Result<R>> successPredicate() {
        return result -> result instanceof TaskTerminalBrainNode.Success<R>;
    }

    public static <R> Predicate<Result<R>> successInnerPredicate(final Predicate<R> predicate) {
        return result -> result instanceof TaskTerminalBrainNode.Success<R> success && predicate.test(success.value);
    }
}
