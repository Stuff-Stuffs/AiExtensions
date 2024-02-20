package io.github.stuff_stuffs.aiex.common.api.brain.behavior;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.EntityType;

import java.util.List;

public interface BehaviorHandlerMap {
    Event<Populate> POPULATION_EVENT = EventFactory.createArrayBacked(Populate.class, events -> (type, helper) -> {
        for (final Populate event : events) {
            event.populate(type, helper);
        }
    });

    <T extends Behavior> List<BehaviorHandler<T>> get(BehaviorType<T> type);

    interface Populate {
        void populate(EntityType<?> type, PopulateHelper helper);
    }

    interface PopulateHelper {
        <T extends Behavior> void accept(BehaviorType<T> type, BehaviorHandler<T> handler);
    }
}
