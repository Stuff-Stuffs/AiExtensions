package io.github.stuff_stuffs.aiex.common.api.brain.behavior;

import io.github.stuff_stuffs.aiex.common.impl.brain.behavior.BehaviorHandlerMapImpl;
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

    <A, R, T extends Behavior.Compound<A, R>> List<BehaviorHandler<A, R, T>> get(BehaviorType<A, R, T> type);

    interface Populate {
        void populate(EntityType<?> type, PopulateHelper helper);
    }

    interface PopulateHelper {
        <A, R, T extends Behavior.Compound<A, R>> void accept(BehaviorType<A, R, T> type, BehaviorHandler<A, R, T> handler);
    }

    static BehaviorHandlerMap create(final EntityType<?> type) {
        return new BehaviorHandlerMapImpl(type);
    }
}
