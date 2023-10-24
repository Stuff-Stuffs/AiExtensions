package io.github.stuff_stuffs.aiex.common.api.brain.memory;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

import java.util.Collection;
import java.util.Optional;

public interface MemoryType<T> {
    RegistryKey<Registry<MemoryType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(AiExCommon.id("memory_types"));
    Registry<MemoryType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();
    Codec<MemoryType<?>> CODEC = REGISTRY.getCodec();

    Codec<T> codec();

    Collection<MemoryReference<?>> insideOf(T value);

    Optional<T> forgetContained(MemoryReference<?> other, T currentValue);

    <K> Optional<T> changeContained(Memory<K> other, K oldValue, T currentValue);
}
