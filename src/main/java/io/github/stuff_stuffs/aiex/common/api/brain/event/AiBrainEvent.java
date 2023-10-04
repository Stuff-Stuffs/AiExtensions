package io.github.stuff_stuffs.aiex.common.api.brain.event;

import com.mojang.serialization.Codec;

public interface AiBrainEvent {
    Codec<AiBrainEvent> CODEC = AiBrainEventType.REGISTRY.getCodec().dispatchStable(AiBrainEvent::type, AiBrainEventType::codec);
    long SECOND = 20;
    long MINUTE = SECOND * 60;
    long HOUR = MINUTE * 60;

    long lifetime();

    AiBrainEventType<?> type();
}
