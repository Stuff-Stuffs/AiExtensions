package io.github.stuff_stuffs.aiex.common.api.brain.task;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public record TaskKey<R, P>(Class<R> resultClass, Class<P> parameterClass) {
    public static final RegistryKey<Registry<TaskKey<?, ?>>> REGISTRY_KEY = RegistryKey.ofRegistry(AiExCommon.id("task_keys"));
    public static final Registry<TaskKey<?, ?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

    public <C> Task<Optional<R>, C> resultReset(final Function<BrainContext<C>, P> parameterFactor, final Predicate<R> reset) {
        return new ResultResetTask<>(this, parameterFactor, reset);
    }

    public <C> Task<Optional<R>, C> contextReset(final Function<BrainContext<C>, P> parameterFactor, final Predicate<BrainContext<C>> reset) {
        return new ContextResetTask<>(this, parameterFactor, reset);
    }
}
