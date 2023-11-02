package io.github.stuff_stuffs.aiex.common.api.brain.memory;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public interface MemoryType<T> {
    RegistryKey<Registry<MemoryType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(AiExCommon.id("memory_types"));
    Registry<MemoryType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();
    Codec<MemoryType<?>> CODEC = REGISTRY.getCodec();

    Codec<T> codec();

    default Collection<? extends MemoryReference<?>> insideOf(final T value) {
        return Collections.emptySet();
    }

    default Optional<T> forgetContained(final MemoryReference<?> other, final T currentValue) {
        return Optional.of(currentValue);
    }

    default <K> Optional<T> changeContained(final Memory<K> other, final K oldValue, final T currentValue) {
        return Optional.of(currentValue);
    }
}
