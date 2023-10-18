package io.github.stuff_stuffs.aiex.common.internal;

import net.minecraft.world.level.storage.LevelStorage;

public interface InternalServerExtensions {
    EntityReferenceContainer aiex$entityRefContainer();

    LevelStorage.Session aiex$session();
}
