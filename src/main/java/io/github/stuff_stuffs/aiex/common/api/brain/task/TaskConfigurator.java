package io.github.stuff_stuffs.aiex.common.api.brain.task;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskConfigurator {
    private final Map<Class<?>, List<ConfiguratorEntry<?>>> configurators;
    private boolean built = false;

    public TaskConfigurator() {
        configurators = new Reference2ObjectOpenHashMap<>();
    }

    public <T> TaskConfigurator add(final Class<T> clazz, final ConfiguratorEntry<T> configuratorEntry) {
        if (built) {
            throw new IllegalStateException();
        }
        configurators.computeIfAbsent(clazz, i -> new ArrayList<>()).add(configuratorEntry);
        return this;
    }

    public void build() {
        if (built) {
            throw new IllegalStateException();
        }
        built = true;
        TaskConfig.ON_BUILD_EVENT.register(new TaskConfig.OnBuild() {
            @Override
            public <T> void onBuild(final T entity, final TaskConfig.Builder<T> builder) {
                for (final Map.Entry<Class<?>, List<ConfiguratorEntry<?>>> entry : configurators.entrySet()) {
                    if (entry.getKey().isInstance(entry)) {
                        for (final ConfiguratorEntry<?> configuratorEntry : entry.getValue()) {
                            cast(entity, builder, configuratorEntry);
                        }
                    }
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T, K> void cast(final K entity, final TaskConfig.Builder<K> builder, final ConfiguratorEntry<T> entry) {
        entry.configure((T) entity, (TaskConfig.Builder<? extends T>) builder, new FactoryAccessor<>() {
            @Override
            public <R, P> void put(final TaskKey<R, P> key, final TaskConfig.Factory<T, R, P> factory) {
                builder.putFactory(key, (TaskConfig.Factory<K, R, P>) factory);
            }

            @Override
            public <R, P> TaskConfig.Factory<T, R, P> get(final TaskKey<R, P> key) {
                return (TaskConfig.Factory<T, R, P>) builder.getFactory(key);
            }

            @Override
            public boolean has(final TaskKey<?, ?> key) {
                return builder.hasFactory(key);
            }
        });
    }

    public interface ConfiguratorEntry<T> {
        void configure(T entity, TaskConfig.Builder<? extends T> builder, FactoryAccessor<T> accessor);
    }

    public interface FactoryAccessor<T> {
        <R, P> void put(TaskKey<R, P> key, TaskConfig.Factory<T, R, P> factory);

        <R, P> TaskConfig.Factory<T, R, P> get(TaskKey<R, P> key);

        boolean has(TaskKey<?, ?> key);
    }
}
