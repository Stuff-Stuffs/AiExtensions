package io.github.stuff_stuffs.aiex.common.api.util;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface AfterRegistryFreezeEvent {
    Event<AfterRegistryFreezeEvent> EVENT = EventFactory.createArrayBacked(AfterRegistryFreezeEvent.class, events -> () -> {
        for (AfterRegistryFreezeEvent event : events) {
            event.afterRegistryFreeze();
        }
    });

    void afterRegistryFreeze();
}
