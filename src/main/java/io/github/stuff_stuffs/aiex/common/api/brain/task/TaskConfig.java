package io.github.stuff_stuffs.aiex.common.api.brain.task;

import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
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

    boolean hasFactory(TaskKey<?, ?, ?> key);

    <R, P, FC> Factory<T, R, P, FC> getFactory(TaskKey<R, P, FC> key);

    static <T> Builder<T> builder() {
        return new TaskConfigImpl.BuilderImpl<>();
    }

    interface Builder<T> {
        boolean hasFactory(TaskKey<?, ?, ?> key);

        <R, P, FC> Factory<T, R, P, FC> getFactory(TaskKey<R, P, FC> key);

        <R, P, FC> void putFactory(TaskKey<R, P, FC> key, Factory<T, R, P, FC> taskFactory);

        TaskConfig<T> build(T entity);
    }

    interface Factory<T, R, P, FC> {
        @Nullable BrainNode<T, R, FC> create(P parameters);

        default Factory<T, R, P, FC> fallbackTo(final Factory<T, R, ? super P, FC> factory) {
            return parameters -> {
                final BrainNode<T, R, FC> task = create(parameters);
                if (task != null) {
                    return task;
                }
                return factory.create(parameters);
            };
        }

        default Factory<T, R, P, FC> fallbackFrom(final Factory<T, R, ? super P, FC> factory) {
            return parameters -> {
                final BrainNode<T, R, FC> task = factory.create(parameters);
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
