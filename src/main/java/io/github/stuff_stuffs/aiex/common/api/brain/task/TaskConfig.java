package io.github.stuff_stuffs.aiex.common.api.brain.task;

import io.github.stuff_stuffs.aiex.common.impl.brain.task.TaskConfigImpl;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public interface TaskConfig<T> {
    Event<OnBuild> ON_BUILD_EVENT = EventFactory.createWithPhases(OnBuild.class, events -> new OnBuild() {
        @Override
        public <T0> void onBuild(final T0 entity, final Builder<T0> builder) {
            for (final OnBuild event : events) {
                event.onBuild(entity, builder);
            }
        }
    }, OnBuild.ADDITION_PHASE, Event.DEFAULT_PHASE, OnBuild.FALLBACK_PHASE, OnBuild.DEFAULTS_PHASE);

    boolean hasFactory(TaskKey<?, ?> key);

    <R, P> Factory<T, R, P> getFactory(TaskKey<R, P> key);

    static <T> Builder<T> builder() {
        return new TaskConfigImpl.BuilderImpl<>();
    }

    interface Builder<T> {
        boolean hasFactory(TaskKey<?, ?> key);

        <R, P> Factory<T, R, P> getFactory(TaskKey<R, P> key);

        <R, P> void putFactory(TaskKey<R, P> key, Factory<T, R, P> taskFactory);

        TaskConfig<T> build(T entity);
    }

    interface Factory<T, R, P> {
        @Nullable Task<R, T> create(P parameters);

        default <P0> Factory<T, R, P0> conditional(final Class<P> clazz) {
            return parameters -> {
                if (clazz.isInstance(parameters)) {
                    //noinspection unchecked
                    return create((P) parameters);
                }
                return null;
            };
        }

        default Factory<T, R, P> fallbackTo(final Factory<T, R, ? super P> factory) {
            return parameters -> {
                final Task<R, T> task = create(parameters);
                if (task != null) {
                    return task;
                }
                return factory.create(parameters);
            };
        }

        default Factory<T, R, P> fallbackFrom(final Factory<T, R, ? super P> factory) {
            return parameters -> {
                final Task<R, T> task = factory.create(parameters);
                if (task != null) {
                    return task;
                }
                return create(parameters);
            };
        }
    }

    interface OnBuild {
        Identifier ADDITION_PHASE = AiExCommon.id("addition");
        Identifier FALLBACK_PHASE = AiExCommon.id("fallback");
        Identifier DEFAULTS_PHASE = AiExCommon.id("defaults");

        <T> void onBuild(T entity, Builder<T> builder);
    }
}
