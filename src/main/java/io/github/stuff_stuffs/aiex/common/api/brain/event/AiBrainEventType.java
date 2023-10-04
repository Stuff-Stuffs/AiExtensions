package io.github.stuff_stuffs.aiex.common.api.brain.event;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

public interface AiBrainEventType<T extends AiBrainEvent> {
    RegistryKey<Registry<AiBrainEventType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(AiExCommon.id("brain_event_types"));
    Registry<AiBrainEventType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

    Codec<T> codec();
}
