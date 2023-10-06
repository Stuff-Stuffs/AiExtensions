package io.github.stuff_stuffs.aiex.common.api.brain.memory;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

import java.util.Optional;
import java.util.Set;

public interface Memory<T> {
    RegistryKey<Registry<Memory<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(AiExCommon.id("memories"));
    Registry<Memory<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

    Optional<Codec<T>> codec();

    Set<Memory<?>> listeningTo();

    Set<Memory<?>> optionalListeningTo();

    <T0> T listenerMemoryUpdate(Memory<T0> memory, T0 oldValue, T0 newValue, T currentVal, AiBrainView brain);
}
