package io.github.stuff_stuffs.aiex.common.internal;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.aiex.common.api.AiExApi;
import io.github.stuff_stuffs.aiex.common.api.AiExGameRules;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEventTypes;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.BasicMemories;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfigurator;
import io.github.stuff_stuffs.aiex.common.api.debug.AiExDebugFlags;
import io.github.stuff_stuffs.aiex.common.api.entity.AiEntity;
import io.github.stuff_stuffs.aiex.common.api.entity.mine.BasicMiningUniverse;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.BasicPathingUniverse;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReferenceDataType;
import io.github.stuff_stuffs.aiex.common.api.util.AfterRegistryFreezeEvent;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import io.github.stuff_stuffs.aiex.common.api.util.tag.DenseRefSet;
import io.github.stuff_stuffs.aiex.common.impl.brain.AiBrainImpl;
import io.github.stuff_stuffs.aiex.common.impl.util.NoopSpannedLoggerImpl;
import io.github.stuff_stuffs.aiex.common.internal.debug.AreaOfInterestDebugMessage;
import io.github.stuff_stuffs.aiex.common.mixin.MixinWorldSavePath;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class AiExCommon implements ModInitializer {
    public static final String MOD_ID = "aiex";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final WorldSavePath ENTITY_REFERENCE_SAVE_PATH = MixinWorldSavePath.callInit("entity_references.dat");
    public static final WorldSavePath ENTITY_LOG_SAVE_PATH = MixinWorldSavePath.callInit("entity_logs");
    public static final AtomicInteger NEXT_BLOCK_ID = new AtomicInteger(0);

    @Override
    public void onInitialize() {
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof AiEntity ai) {
                ((AiBrainImpl<?>) ai.aiex$getBrain()).logger().close();
            }
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> resetDenseTags());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> resetDenseTags());
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> resetDenseTags());
        Registry.register(LocationClassifier.REGISTRY, AiExCommon.id("npc_basic"), BasicPathingUniverse.CLASSIFIER);
        Registry.register(LocationClassifier.REGISTRY, AiExCommon.id("npc_basic_mining"), BasicMiningUniverse.CLASSIFIER);
        AiExApi.init();
        AiBrainEventTypes.init();
        AiExGameRules.init();
        BasicTasks.init();
        BrainConfig.Key.init();
        EntityReferenceDataType.REGISTRY.getCodec();
        AiExDebugFlags.init();
        AiExCommands.init();
        BasicMemories.init();
        AfterRegistryFreezeEvent.EVENT.register(EntityReferenceDataTypeCache::clear);
        Registry.register(AiExDebugFlags.REGISTRY, id("aoi"), AreaOfInterestDebugMessage.FLAG);
        TaskConfigurator.init();
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof AiEntity ai) {
                ai.aiex$getBrain().unload();
            }
        });
    }

    public static <B, T extends B> TypeFilter<B, T> createDelegatingTypeFilter(final Class<T> cls) {
        return new TypeFilter<>() {
            @Nullable
            @Override
            public T downcast(final B obj) {
                if (cls.isInstance(obj)) {
                    return (T) obj;
                }
                if (obj instanceof AiEntity entity) {
                    if (entity.aiex$getBrain().hasFakePlayerDelegate() && cls.isInstance(entity.aiex$getBrain().fakePlayerDelegate())) {
                        return (T) entity.aiex$getBrain().fakePlayerDelegate();
                    }
                }
                return null;
            }

            @Override
            public Class<? extends B> getBaseClass() {
                return cls;
            }
        };
    }

    private static void resetDenseTags() {
        DenseRefSet.resetAll();
    }

    public static SpannedLogger createForEntity(final Entity entity) {
        if (entity.getEntityWorld() instanceof ServerWorld serverWorld) {
            try {
                final LevelStorage.Session session = ((InternalServerExtensions) serverWorld.getServer()).aiex$session();
                final Path directory = Files.createDirectories(session.getDirectory(ENTITY_LOG_SAVE_PATH));
                final Path path = directory.resolve(entity.getUuidAsString() + ".log.xml.gz");
                return SpannedLogger.create(SpannedLogger.Level.WARNING, "Entity", path);
            } catch (final IOException ignored) {
            }
        }
        return new NoopSpannedLoggerImpl();
    }

    public static Identifier id(final String path) {
        return new Identifier(MOD_ID, path);
    }
}