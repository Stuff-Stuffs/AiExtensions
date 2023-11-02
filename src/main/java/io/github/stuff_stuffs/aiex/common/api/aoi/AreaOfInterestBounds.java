package io.github.stuff_stuffs.aiex.common.api.aoi;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.aiex.common.internal.aoi.AoiSectionPos;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.stream.IntStream;

public record AreaOfInterestBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public static final Codec<AreaOfInterestBounds> CODEC = Codec.INT_STREAM.comapFlatMap(stream ->
                    Util.decodeFixedLengthArray(stream, 6).map(arr -> new AreaOfInterestBounds(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5])),
            bounds -> IntStream.of(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ)
    );
    public static final int BUFFER = 4;
    public static final int MAX_SIZE = (1 << AoiSectionPos.CHUNKS_PER_SECTION) * BUFFER;

    public AreaOfInterestBounds {
        if (!(minX < maxX) || !(minY < maxY) || !(minZ < maxZ)) {
            throw new IllegalArgumentException();
        }
    }

    public BlockPos center() {
        return new BlockPos(minX + (maxX-minX)/2, minY + (maxY-minY)/2, maxZ + (maxZ-minZ)/2);
    }

    public static AreaOfInterestBounds fromDimensions(final int x, final int y, final int z, final int xL, final int yL, final int zL) {
        return new AreaOfInterestBounds(x, y, z, x + xL, y + yL, z + zL);
    }

    public boolean checkValidSize() {
        return maxX - minX <= MAX_SIZE && maxY - minY <= MAX_SIZE && maxZ - minZ <= MAX_SIZE;
    }

    public boolean intersects(final AreaOfInterestBounds o) {
        return o.minX < maxX && minX < o.maxX
                && o.minY < maxY && minY < o.maxY
                && o.minZ < maxZ && minZ < o.maxZ;
    }

    public AreaOfInterestBounds union(final AreaOfInterestBounds other) {
        return new AreaOfInterestBounds(
                Math.min(minX, other.minX),
                Math.min(minY, other.minY),
                Math.min(minZ, other.minZ),
                Math.max(maxX, other.maxX),
                Math.max(maxY, other.maxY),
                Math.max(maxZ, other.maxZ)
        );
    }

    public int length(final Direction.Axis axis) {
        return switch (axis) {
            case X -> maxX - minX;
            case Y -> maxY - minY;
            case Z -> maxZ - minZ;
        };
    }

    public int min(final Direction.Axis axis) {
        return switch (axis) {
            case X -> minX;
            case Y -> minY;
            case Z -> minZ;
        };
    }

    public int max(final Direction.Axis axis) {
        return switch (axis) {
            case X -> maxX;
            case Y -> maxY;
            case Z -> maxZ;
        };
    }

    public Box box() {
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
