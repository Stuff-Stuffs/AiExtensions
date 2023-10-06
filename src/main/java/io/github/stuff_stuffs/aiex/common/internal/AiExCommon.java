package io.github.stuff_stuffs.aiex.common.internal;

import io.github.stuff_stuffs.aiex.common.api.AiExGameRules;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEventTypes;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memories;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReferenceDataType;
import io.github.stuff_stuffs.aiex.common.mixin.MixinWorldSavePath;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiExCommon implements ModInitializer {
    public static final String MOD_ID = "aiex";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final WorldSavePath ENTITY_REFERENCE_SAVE_PATH = MixinWorldSavePath.callInit("entity_references.dat");

    @Override
    public void onInitialize() {
        AiBrainEventTypes.init();
        AiExGameRules.init();
        Memories.init();
        EntityReferenceDataType.REGISTRY.getCodec();
        EntityReferenceDataTypeCache.clear();
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> entity.aiex$getAndUpdateReference());
    }

    public static Identifier id(final String path) {
        return new Identifier(MOD_ID, path);
    }
}