package io.github.stuff_stuffs.aiex.common.api.util;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.ShapeCache;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.block.BlockState;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.Nullable;

public abstract class CollisionHelper<B, F, R> {
    private static final int CACHE_BITS = 4;
    private static final int CACHE_SIZE = 1 << CACHE_BITS;
    private static final int CACHE_MASK = CACHE_SIZE - 1;
    protected static final VoxelShape FULL_CUBE = VoxelShapes.fullCube();
    protected static final VoxelShape EMPTY = VoxelShapes.empty();
    protected final Box box;
    protected final Box floorBox;
    protected final VoxelShape boxShape;
    protected final VoxelShape floorShape;
    protected final B emptyBoxState;
    protected final F emptyFloorState;
    private final double[] cachedHeights;
    private final VoxelShape[] cachedBoxShapes;
    private final VoxelShape[] cachedFloorShapes;

    public R test(final int x, final int y, final int z, final ShapeCache world) {
        final double floorOffset = floorHeight(x, y, z, world);
        final VoxelShape shape;
        final VoxelShape floorShape;
        if (floorOffset == 0.0) {
            shape = boxShape;
            floorShape = this.floorShape;
        } else {
            final int index = (int) (HashCommon.mix(Double.doubleToRawLongBits(floorOffset)) >>> 16) & CACHE_MASK;
            if (cachedHeights[index] == floorOffset) {
                shape = cachedBoxShapes[index];
                floorShape = cachedFloorShapes[index];
            } else {
                cachedHeights[index] = floorOffset;
                shape = cachedBoxShapes[index] = boxShape.offset(0, floorOffset, 0);
                floorShape = cachedFloorShapes[index] = this.floorShape.offset(0, floorOffset, 0);
            }
        }
        final B box = testBox(x, y + floorOffset, z, world, shape);
        final R collision = boxCollision(box);
        if (collision != null) {
            return collision;
        } else {
            final F floor = testFloor(x, y + floorOffset, z, world, floorShape);
            return bothCollision(box, floor);
        }
    }

    protected abstract @Nullable R boxCollision(B box);

    protected abstract R bothCollision(B box, F floor);

    protected double floorHeight(final int xOff, final int yOff, final int zOff, final ShapeCache cache) {
        final int minX = MathHelper.floor(floorBox.minX + xOff);
        final int maxX = MathHelper.ceil(floorBox.maxX + xOff + 0.0000001) - 1;
        final int minY = MathHelper.floor(floorBox.minY + yOff);
        final int minZ = MathHelper.floor(floorBox.minZ + zOff);
        final int maxZ = MathHelper.ceil(floorBox.maxZ + zOff + 0.0000001) - 1;
        double maxFloorHeight = 0.0;
        for (int x = minX; x <= maxX; ++x) {
            for (int z = minZ; z <= maxZ; ++z) {
                final VoxelShape shape = cache.getCollisionShape(x, minY, z);
                maxFloorHeight = Math.max(maxFloorHeight, shape.getMax(Direction.Axis.Y));
                if (maxFloorHeight == 1) {
                    return 0.0;
                }
            }
        }
        return maxFloorHeight;
    }

    protected abstract boolean shouldFloorReturnEarly(F state);

    protected abstract F updateFloorState(F old, int bx, int by, int bz, double x, double y, double z, VoxelShape thisShape, VoxelShape shape, BlockState state, ShapeCache world);

    private F testFloor(final double xOff, final double yOff, final double zOff, final ShapeCache world, final VoxelShape shape) {
        final int minX = MathHelper.floor(floorBox.minX + xOff);
        final int maxX = MathHelper.ceil(floorBox.maxX + xOff + 0.0000001) - 1;
        final int minY = MathHelper.floor(floorBox.minY + yOff) - 1;
        final int maxY = MathHelper.ceil(floorBox.maxY + yOff + 0.0000001) - 1;
        final int minZ = MathHelper.floor(floorBox.minZ + zOff);
        final int maxZ = MathHelper.ceil(floorBox.maxZ + zOff + 0.0000001) - 1;
        F collisionState = emptyFloorState;
        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    final BlockState state = world.getBlockState(x, y, z);
                    final VoxelShape voxelShape = world.getCollisionShape(x, y, z);
                    collisionState = updateFloorState(collisionState, x, y, z, xOff, yOff, zOff, shape, voxelShape, state, world);
                    if (shouldFloorReturnEarly(collisionState)) {
                        return collisionState;
                    }
                }
            }
        }
        return collisionState;
    }

    protected static boolean checkFloorCollision(final Box thisBox, final VoxelShape thisShape, final VoxelShape blockShape, final int bx, final int by, final int bz, final double x, final double y, final double z) {
        if (blockShape != EMPTY && !blockShape.isEmpty()) {
            if (blockShape == FULL_CUBE) {
                final double xBox = (double) bx - x;
                final double yBox = 1 + (double) by - y;
                final double zBox = (double) bz - z;
                return thisBox.intersects(xBox, yBox, zBox, xBox + 1.0D, yBox + 1.0D, zBox + 1.0D);
            } else {
                return VoxelShapes.matchesAnywhere(thisShape, blockShape, BooleanBiFunction.AND);
            }
        }
        return false;
    }

    protected abstract boolean shouldReturnEarly(B state);

    protected abstract B updateState(B old, int bx, int by, int bz, double x, double y, double z, VoxelShape thisShape, VoxelShape shape, BlockState state, ShapeCache world);

    protected static boolean checkBoxCollision(final Box thisBox, final VoxelShape thisShape, final VoxelShape blockShape, final int bx, final int by, final int bz, final double x, final double y, final double z) {
        if (blockShape != EMPTY && !blockShape.isEmpty()) {
            if (blockShape == FULL_CUBE) {
                final double xBox = (double) bx - x;
                final double yBox = (double) by - y;
                final double zBox = (double) bz - z;
                return thisBox.intersects(xBox, yBox, zBox, xBox + 1.0D, yBox + 1.0D, zBox + 1.0D);
            } else {
                return VoxelShapes.matchesAnywhere(thisShape, blockShape, BooleanBiFunction.AND);
            }
        }
        return false;
    }

    private B testBox(final double xOff, final double yOff, final double zOff, final ShapeCache world, final VoxelShape shape) {
        final Box box = this.box;
        final int minX = MathHelper.floor(box.minX + xOff);
        final int maxX = MathHelper.ceil(box.maxX + xOff + 0.0000001) - 1;
        final int minY = MathHelper.floor(box.minY + yOff);
        final int maxY = MathHelper.ceil(box.maxY + yOff + 0.0000001);
        final int minZ = MathHelper.floor(box.minZ + zOff);
        final int maxZ = MathHelper.ceil(box.maxZ + zOff + 0.0000001) - 1;
        B collisionState = emptyBoxState;

        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    final BlockState state = world.getBlockState(x, y, z);
                    final VoxelShape voxelShape = world.getCollisionShape(x, y, z);
                    collisionState = updateState(collisionState, x, y, z, xOff, yOff, zOff, shape, voxelShape, state, world);
                    if (shouldReturnEarly(collisionState)) {
                        return collisionState;
                    }
                }
            }
        }
        return collisionState;
    }

    public CollisionHelper(final double width, final double height, final B emptyBoxState, final F emptyFloorState) {
        this.emptyBoxState = emptyBoxState;
        this.emptyFloorState = emptyFloorState;
        final double width2 = width * 0.5;
        box = new Box(0.5 - width2, 0.0D, 0.5 - width2, 0.5 + width2, height, 0.5 + width2);
        floorBox = new Box(0.5 - width2, 15 / 16.0, 0.5 - width2, 0.5 + width2, 1.0, 0.5 + width2);
        boxShape = VoxelShapes.cuboid(box);
        floorShape = VoxelShapes.cuboid(floorBox);
        cachedHeights = new double[CACHE_SIZE];
        for (int i = 0; i < CACHE_SIZE; i++) {
            cachedHeights[i] = Double.NaN;
        }
        cachedBoxShapes = new VoxelShape[CACHE_SIZE];
        cachedFloorShapes = new VoxelShape[CACHE_SIZE];
    }
}
