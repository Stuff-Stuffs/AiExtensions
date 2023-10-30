package io.github.stuff_stuffs.aiex.common.internal;

import io.github.stuff_stuffs.aiex.common.api.AiExApi;
import net.minecraft.world.level.storage.LevelStorage;

public interface InternalServerExtensions {
    EntityReferenceContainer aiex$entityRefContainer();

    LevelStorage.Session aiex$session();

    void aiex$submitTask(AiExApi.Job task);
}
