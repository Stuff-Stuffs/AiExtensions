package io.github.stuff_stuffs.aiex.common.api;

import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReference;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface AiWorldExtensions {
    @Nullable EntityReference aiex$getEntityReference(UUID uuid);

    static AiWorldExtensions get(final ServerWorld world) {
        return (AiWorldExtensions) world;
    }
}
