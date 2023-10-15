package io.github.stuff_stuffs.aiex.common.api.brain.event;

import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.minecraft.registry.Registry;

public final class AiBrainEventTypes {
    public static final AiBrainEventType<DamagedBrainEvent> DAMAGED = () -> DamagedBrainEvent.CODEC;
    public static final AiBrainEventType<ObservedEntityBrainEvent> OBSERVED_ENTITY = () -> ObservedEntityBrainEvent.CODEC;
    public static final AiBrainEventType<ObservedProjectileEntityBrainEvent> OBSERVED_PROJECTILE_ENTITY = () -> ObservedProjectileEntityBrainEvent.CODEC;

    public static void init() {
        Registry.register(AiBrainEventType.REGISTRY, AiExCommon.id("damaged"), DAMAGED);
        Registry.register(AiBrainEventType.REGISTRY, AiExCommon.id("observed_entity"), OBSERVED_ENTITY);
        Registry.register(AiBrainEventType.REGISTRY, AiExCommon.id("observed_projectile_entity"), OBSERVED_PROJECTILE_ENTITY);
    }

    private AiBrainEventTypes() {
    }
}
