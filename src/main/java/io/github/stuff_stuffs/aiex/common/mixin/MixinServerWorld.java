package io.github.stuff_stuffs.aiex.common.mixin;

import io.github.stuff_stuffs.aiex.common.api.AiWorldExtensions;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestType;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestWorld;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReference;
import io.github.stuff_stuffs.aiex.common.impl.aoi.AreaOfInterestWorldImpl;
import io.github.stuff_stuffs.aiex.common.internal.InternalServerExtensions;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.spawner.SpecialSpawner;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld extends World implements AiWorldExtensions {
    @Unique
    private AreaOfInterestWorldImpl aiex$aoiWorld;

    protected MixinServerWorld(final MutableWorldProperties properties, final RegistryKey<World> registryRef, final DynamicRegistryManager registryManager, final RegistryEntry<DimensionType> dimensionEntry, final Supplier<Profiler> profiler, final boolean isClient, final boolean debugWorld, final long biomeAccess, final int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
    }

    @Override
    @Shadow
    public abstract MinecraftServer getServer();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initHook(final MinecraftServer server, final Executor workerExecutor, final LevelStorage.Session session, final ServerWorldProperties properties, final RegistryKey<World> worldKey, final DimensionOptions dimensionOptions, final WorldGenerationProgressListener worldGenerationProgressListener, final boolean debugWorld, final long seed, final List<SpecialSpawner> spawners, final boolean shouldTickTime, final RandomSequencesState randomSequencesState, final CallbackInfo ci) {
        aiex$aoiWorld = new AreaOfInterestWorldImpl(session.getWorldDirectory(worldKey), worldKey, getBottomY(), getHeight());
    }

    @Override
    public @Nullable EntityReference aiex$getEntityReference(final UUID uuid) {
        return ((InternalServerExtensions) getServer()).aiex$entityRefContainer().get(uuid);
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void closeHook(final CallbackInfo ci) {
        aiex$aoiWorld.close();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void tickHook(final BooleanSupplier shouldKeepTicking, final CallbackInfo ci) {
        aiex$aoiWorld.tick((ServerWorld) (Object) this);
    }

    @Override
    public void aiex$resyncAreaOfInterest(final AreaOfInterestType<?> type) {
        aiex$aoiWorld.resync((ServerWorld) (Object) this, type);
    }

    @Override
    public AreaOfInterestWorld aiex$getAoiWorld() {
        return aiex$aoiWorld;
    }
}
