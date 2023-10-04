package io.github.stuff_stuffs.aiex.common.api.brain.task;

import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

public record TaskKey<R, P>(Class<R> resultClass, Class<P> parameterClass) {
    public static final RegistryKey<Registry<TaskKey<?, ?>>> REGISTRY_KEY = RegistryKey.ofRegistry(AiExCommon.id("task_keys"));
    public static final Registry<TaskKey<?, ?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();
}
