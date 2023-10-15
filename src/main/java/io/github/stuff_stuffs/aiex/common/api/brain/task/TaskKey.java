package io.github.stuff_stuffs.aiex.common.api.brain.task;

import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

public record TaskKey<R, P, FC>(Class<R> resultClass, Class<P> parameterClass, Class<FC> argClass) {
    public static final RegistryKey<Registry<TaskKey<?, ?, ?>>> REGISTRY_KEY = RegistryKey.ofRegistry(AiExCommon.id("task_keys"));
    public static final Registry<TaskKey<?, ?, ?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

    @Override
    public boolean equals(final Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        int result = resultClass.hashCode();
        result = 31 * result + parameterClass.hashCode();
        return result;
    }
}
