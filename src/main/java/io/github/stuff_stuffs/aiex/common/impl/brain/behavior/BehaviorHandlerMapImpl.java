package io.github.stuff_stuffs.aiex.common.impl.brain.behavior;

import io.github.stuff_stuffs.aiex.common.api.brain.behavior.Behavior;
import io.github.stuff_stuffs.aiex.common.api.brain.behavior.BehaviorHandler;
import io.github.stuff_stuffs.aiex.common.api.brain.behavior.BehaviorHandlerMap;
import io.github.stuff_stuffs.aiex.common.api.brain.behavior.BehaviorType;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BehaviorHandlerMapImpl implements BehaviorHandlerMap {
    private final Map<BehaviorType<?, ?, ?>, List<BehaviorHandler<?, ?, ?>>> map;

    public BehaviorHandlerMapImpl(final EntityType<?> type) {
        map = new Reference2ObjectOpenHashMap<>();
        POPULATION_EVENT.invoker().populate(type, new PopulateHelper() {
            @Override
            public <A, R, T extends Behavior.Compound<A, R>> void accept(final BehaviorType<A, R, T> type, final BehaviorHandler<A, R, T> handler) {
                map.computeIfAbsent(type, t -> new ArrayList<>()).add(handler);
            }
        });
    }

    @Override
    public <A, R, T extends Behavior.Compound<A, R>> List<BehaviorHandler<A, R, T>> get(final BehaviorType<A, R, T> type) {
        final List<? extends BehaviorHandler<?, ?, ?>> handlers = map.get(type);
        if (handlers == null) {
            return List.of();
        }
        //noinspection unchecked
        return (List<BehaviorHandler<A, R, T>>) handlers;
    }
}
