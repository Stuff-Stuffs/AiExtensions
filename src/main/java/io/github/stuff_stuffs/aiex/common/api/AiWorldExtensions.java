package io.github.stuff_stuffs.aiex.common.api;

import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestType;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestWorld;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReference;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface AiWorldExtensions {
    @Nullable EntityReference aiex$getEntityReference(UUID uuid);

    AreaOfInterestWorld aiex$getAoiWorld();

    void aiex$resyncAreaOfInterest(AreaOfInterestType<?> type);

    static AiWorldExtensions get(final ServerWorld world) {
        return (AiWorldExtensions) world;
    }
}
