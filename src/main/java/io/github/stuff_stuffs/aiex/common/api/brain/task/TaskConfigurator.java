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
                    if (entry.getKey().isInstance(entity)) {
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
            public <R, P, FC> void put(final TaskKey<R, P, FC> key, final TaskConfig.Factory<T, R, P, FC> factory) {
                builder.putFactory(key, (TaskConfig.Factory<K, R, P, FC>) factory);
            }

            @Override
            public <R, P, FC> TaskConfig.Factory<T, R, P, FC> get(final TaskKey<R, P, FC> key) {
                return (TaskConfig.Factory<T, R, P, FC>) builder.getFactory(key);
            }

            @Override
            public boolean has(final TaskKey<?, ?, ?> key) {
                return builder.hasFactory(key);
            }
        });
    }

    public interface ConfiguratorEntry<T> {
        void configure(T entity, TaskConfig.Builder<? extends T> builder, FactoryAccessor<T> accessor);
    }

    public interface FactoryAccessor<T> {
        <R, P, FC> void put(TaskKey<R, P, FC> key, TaskConfig.Factory<T, R, P, FC> factory);

        <R, P, FC> TaskConfig.Factory<T, R, P, FC> get(TaskKey<R, P, FC> key);

        boolean has(TaskKey<?, ?, ?> key);
    }
}
