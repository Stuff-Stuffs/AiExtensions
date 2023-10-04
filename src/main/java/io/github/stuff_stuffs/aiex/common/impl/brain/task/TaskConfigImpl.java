package io.github.stuff_stuffs.aiex.common.impl.brain.task;

import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskKey;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.Map;

public class TaskConfigImpl<T> implements TaskConfig<T> {
    private final Map<TaskKey<?, ?>, Factory<T, ?, ?>> map;

    public TaskConfigImpl(final Map<TaskKey<?, ?>, Factory<T, ?, ?>> map) {
        this.map = Map.copyOf(map);
    }

    @Override
    public boolean hasFactory(final TaskKey<?, ?> key) {
        return map.containsKey(key);
    }

    @Override
    public <R, P> Factory<T, R, P> getFactory(final TaskKey<R, P> key) {
        final Factory<T, ?, ?> function = map.get(key);
        if (function == null) {
            throw new NullPointerException();
        }
        //noinspection unchecked
        return (Factory<T, R, P>) function;
    }

    public static final class BuilderImpl<T> implements TaskConfig.Builder<T> {
        private final Map<TaskKey<?, ?>, Factory<T, ?, ?>> map;

        public BuilderImpl() {
            map = new Reference2ObjectOpenHashMap<>();
        }

        public BuilderImpl<T> copy() {
            final BuilderImpl<T> builder = new BuilderImpl<>();
            builder.map.putAll(map);
            return builder;
        }

        @Override
        public boolean hasFactory(final TaskKey<?, ?> key) {
            return map.containsKey(key);
        }

        @Override
        public <R, P> Factory<T, R, P> getFactory(final TaskKey<R, P> key) {
            final Factory<T, ?, ?> function = map.get(key);
            if (function == null) {
                throw new NullPointerException();
            }
            //noinspection unchecked
            return (Factory<T, R, P>) function;
        }

        @Override
        public <R, P> void putFactory(final TaskKey<R, P> key, final Factory<T, R, P> taskFactory) {
            map.put(key, taskFactory);
        }

        @Override
        public TaskConfig<T> build(final T entity) {
            final BuilderImpl<T> copy = copy();
            TaskConfig.ON_BUILD_EVENT.invoker().onBuild(entity, copy);
            return new TaskConfigImpl<>(copy.map);
        }
    }
}
