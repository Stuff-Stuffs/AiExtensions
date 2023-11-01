package io.github.stuff_stuffs.aiex.common.internal.aoi;

import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestBounds;
import net.minecraft.util.math.ChunkPos;

public record AoiSectionPos(int x, int z) {
    public static final int CHUNKS_PER_SECTION = 8;

    public static AoiSectionPos from(final ChunkPos pos) {
        return new AoiSectionPos(pos.x / CHUNKS_PER_SECTION, pos.z / CHUNKS_PER_SECTION);
    }

    public ChunkPos lower() {
        return new ChunkPos(x * CHUNKS_PER_SECTION, z * CHUNKS_PER_SECTION);
    }

    public ChunkPos upper() {
        return new ChunkPos(x * CHUNKS_PER_SECTION + CHUNKS_PER_SECTION - 1, z * CHUNKS_PER_SECTION + CHUNKS_PER_SECTION - 1);
    }

    public static AoiSectionPos[] from(final AreaOfInterestBounds bounds) {
        final AoiSectionPos lower = AoiSectionPos.from(new ChunkPos(bounds.minX() >> 4, bounds.minZ() >> 4));
        final AoiSectionPos upper = AoiSectionPos.from(new ChunkPos((bounds.maxX() - 1) >> 4, (bounds.maxZ() - 1) >> 4));
        final int buffer = AreaOfInterestBounds.BUFFER;
        final int size = (1 + (upper.x + buffer) - (lower.x - buffer)) * (1 + (upper.z + buffer) - (lower.z - buffer));
        final AoiSectionPos[] arr = new AoiSectionPos[size];
        int idx = 0;
        for (int i = lower.x - buffer; i <= upper.x + buffer; i++) {
            for (int j = lower.z - buffer; j <= upper.z + buffer; j++) {
                arr[idx++] = new AoiSectionPos(i, j);
            }
        }
        return arr;
    }
}
