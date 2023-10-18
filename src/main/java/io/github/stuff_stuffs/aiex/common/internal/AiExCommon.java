package io.github.stuff_stuffs.aiex.common.internal;

import io.github.stuff_stuffs.aiex.common.api.AiExApi;
import io.github.stuff_stuffs.aiex.common.api.AiExGameRules;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEventTypes;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memories;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.entity.AiEntity;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReferenceDataType;
import io.github.stuff_stuffs.aiex.common.api.util.AfterRegistryFreezeEvent;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import io.github.stuff_stuffs.aiex.common.impl.brain.AiBrainImpl;
import io.github.stuff_stuffs.aiex.common.impl.util.NoopSpannedLoggerImpl;
import io.github.stuff_stuffs.aiex.common.mixin.MixinWorldSavePath;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AiExCommon implements ModInitializer {
    public static final String MOD_ID = "aiex";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final WorldSavePath ENTITY_REFERENCE_SAVE_PATH = MixinWorldSavePath.callInit("entity_references.dat");
    public static final WorldSavePath ENTITY_LOG_SAVE_PATH = MixinWorldSavePath.callInit("entity_logs");

    @Override
    public void onInitialize() {
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof AiEntity ai) {
                ((AiBrainImpl<?>) ai.aiex$getBrain()).logger().close();
            }
        });
        AiExApi.init();
        AiBrainEventTypes.init();
        AiExGameRules.init();
        Memories.init();
        BasicTasks.init();
        BrainConfig.Key.init();
        EntityReferenceDataType.REGISTRY.getCodec();
        AfterRegistryFreezeEvent.EVENT.register(EntityReferenceDataTypeCache::clear);
    }

    public static SpannedLogger createForEntity(final Entity entity) {
        if (entity.getEntityWorld() instanceof ServerWorld serverWorld) {
            try {
                final LevelStorage.Session session = ((InternalServerExtensions) serverWorld.getServer()).aiex$session();
                final Path directory = Files.createDirectories(session.getDirectory(ENTITY_LOG_SAVE_PATH));
                final Path path = directory.resolve(entity.getUuidAsString() + ".xml");
                return SpannedLogger.create(SpannedLogger.Level.DEBUG, "Entity", path);
            } catch (final IOException ignored) {
            }
        }
        return new NoopSpannedLoggerImpl();
    }

    public static Identifier id(final String path) {
        return new Identifier(MOD_ID, path);
    }
}