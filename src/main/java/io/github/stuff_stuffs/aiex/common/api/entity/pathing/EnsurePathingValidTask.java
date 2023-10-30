package io.github.stuff_stuffs.aiex.common.api.entity.pathing;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai_pathing.common.impl.job.LocationCachingJob;
import io.github.stuff_stuffs.advanced_ai_pathing.common.internal.extensions.ChunkSectionExtensions;
import io.github.stuff_stuffs.advanced_ai_pathing.common.internal.extensions.ServerExtensions;
import io.github.stuff_stuffs.aiex.common.api.AiExApi;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class EnsurePathingValidTask implements AiExApi.Job {
    private final ServerWorld world;
    private final ChunkSectionPos min;
    private final ChunkSectionPos max;
    private final List<LocationClassifier<?>> classifiers;
    private Chunk[] chunks;

    public EnsurePathingValidTask(final ServerWorld world, final ChunkSectionPos min, final ChunkSectionPos max, final Collection<LocationClassifier<?>> classifiers) {
        this.world = world;
        this.min = min;
        this.max = max;
        this.classifiers = List.copyOf(classifiers);
    }

    @Override
    public void run() {
        if (chunks == null) {
            return;
        }
        for (final Chunk chunk : chunks) {
            final int sectionX = chunk.getPos().x;
            final int sectionZ = chunk.getPos().z;
            for (int sectionY = min.getSectionY(); sectionY <= max.getSectionY(); sectionY++) {
                maybeEnqueue(sectionX, sectionY, sectionZ, chunk);
            }
        }
    }

    @Override
    public void preRun() {
        final List<Chunk> chunks = new ArrayList<>();
        for (int sectionX = min.getSectionX(); sectionX <= max.getSectionX(); sectionX++) {
            for (int sectionZ = min.getSectionZ(); sectionZ <= max.getSectionZ(); sectionZ++) {
                final Chunk chunk = world.getChunk(sectionX, sectionZ, ChunkStatus.FULL, false);
                if (chunk != null) {
                    chunks.add(chunk);
                }
            }
        }
        this.chunks = chunks.toArray(new Chunk[0]);
    }

    private void maybeEnqueue(final int sectionX, final int sectionY, final int sectionZ, final Chunk chunk) {
        if (world.getBottomSectionCoord() <= sectionY && sectionY < world.getTopSectionCoord()) {
            final ChunkSection section = chunk.getSection(world.sectionCoordToIndex(sectionY));
            if (section != null) {
                for (final LocationClassifier<?> classifier : classifiers) {
                    if (((ChunkSectionExtensions) section).advanced_ai_pathing$sectionData().getLocationCache(classifier) == null) {
                        ((ServerExtensions) world.getServer()).advanced_ai_pathing$executor().enqueue(new LocationCachingJob<>(ChunkSectionPos.from(sectionX, sectionY, sectionZ), world, classifier));
                    }
                }
            }
        }
    }
}
