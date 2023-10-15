package io.github.stuff_stuffs.aiex.common.api.brain.util.target;

import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEvent;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEventType;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memory;
import net.minecraft.entity.Entity;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public final class Targeting {
    public static <C, R, FC, T> Targeter<C, Optional<R>, FC> memoryTarget(final Memory<T> memory, final BiFunction<T, FC, Optional<R>> factory, final boolean dynamic) {
        return new AbstractCachingTargeter<>(dynamic) {
            @Override
            protected Optional<R> query(final BrainContext<C> context, final FC arg) {
                final AiBrainView.Memories memories = context.brain().memories();
                if (!memories.has(memory)) {
                    return Optional.empty();
                }
                return factory.apply(memories.get(memory).get(), arg);
            }
        };
    }

    public static <C, R, FC, T> Targeter<C, Optional<R>, FC> memoryTarget(final BiFunction<BrainContext<C>, FC, Memory<T>> memoryFunc, final BiFunction<T, FC, Optional<R>> factory, final boolean dynamic) {
        return new AbstractCachingTargeter<>(dynamic) {
            @Override
            protected Optional<R> query(final BrainContext<C> context, final FC arg) {
                final Memory<T> memory = memoryFunc.apply(context, arg);
                final AiBrainView.Memories memories = context.brain().memories();
                if (!memories.has(memory)) {
                    return Optional.empty();
                }
                return factory.apply(memories.get(memory).get(), arg);
            }
        };
    }

    public static <C extends Entity, E extends Entity, FC> Targeter<C, Optional<E>, FC> entityTarget(final TypeFilter<Entity, E> typeFilter, final EntityFilter<C, E, FC> filter, final boolean dynamic, final int range) {
        return new AbstractCachingTargeter<>(dynamic) {
            @Override
            protected Optional<E> query(final BrainContext<C> context, final FC arg) {
                final List<E> entities = new ArrayList<>(1);
                context.world().collectEntitiesByType(typeFilter, Box.of(context.entity().getPos(), range * 2, range * 2, range * 2), e -> filter.test(context, arg, e), entities, 1);
                if (entities.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(entities.get(0));
            }
        };
    }

    public static <C, R, FC, E extends AiBrainEvent> Targeter<C, Optional<R>, FC> eventTarget(final AiBrainEventType<E> type, final EventTarget<C, R, FC, E> extractor, final long bufferTime, final boolean oldest, final boolean dynamic) {
        return new AbstractCachingTargeter<>(dynamic) {
            @Override
            protected Optional<R> query(final BrainContext<C> context, final FC arg) {
                final AiBrainView.Events events = context.brain().events();
                final Stream<E> query = events.streamQuery(type, context.brain().age() - bufferTime, !oldest);
                return extractor.apply(context, arg, query);
            }
        };
    }

    public interface EventTarget<C, R, FC, E> {
        Optional<R> apply(BrainContext<C> context, FC arg, Stream<E> stream);
    }

    public interface EntityFilter<C, E extends Entity, FC> {
        boolean test(BrainContext<C> context, FC arg, E entity);
    }

    private Targeting() {
    }
}