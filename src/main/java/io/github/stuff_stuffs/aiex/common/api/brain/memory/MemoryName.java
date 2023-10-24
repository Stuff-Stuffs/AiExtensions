package io.github.stuff_stuffs.aiex.common.api.brain.memory;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

public interface MemoryName<T> {
    RegistryKey<Registry<MemoryName<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(AiExCommon.id("memory_names"));
    Registry<MemoryName<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();
    Codec<MemoryName<?>> CODEC = REGISTRY.getCodec();

    MemoryType<T> type();
}
