package io.github.stuff_stuffs.aiex.common.api.util;

import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import net.minecraft.block.BlockState;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.Nullable;

public abstract class FlaggedCollisionHelper<T, R> {
    private static final VoxelShape FULL_CUBE = VoxelShapes.fullCube();
    private static final VoxelShape EMPTY = VoxelShapes.empty();
    private final Box box;
    private final Box floorBox;
    private final VoxelShape boxShape;
    private final VoxelShape floorShape;
    private final R openFlag;
    private final R closedFlag;
    private final R floorFlag;
    private @Nullable T flag = null;

    public R test(final int x, final int y, final int z, final ShapeCache world) {
        flag = null;
        final double floorOffset = floorHeight(x, y, z, world);
        if (testBox(x, y + floorOffset, z, world)) {
            if (flag == null) {
                return closedFlag;
            } else {
                return combineWithClose(flag);
            }
        } else {
            if (testFloor(x, y + floorOffset, z, world)) {
                if (flag == null) {
                    return floorFlag;
                } else {
                    return combineWithFloor(flag);
                }
            } else {
                if (flag == null) {
                    return openFlag;
                } else {
                    return combineWithOpen(flag);
                }
            }
        }
    }

    protected abstract R combineWithOpen(T flag);

    protected abstract R combineWithClose(T flag);

    protected abstract R combineWithFloor(T flag);

    protected abstract @Nullable T testFloorState(BlockState state, int x, int y, int z, ShapeCache world);

    protected abstract @Nullable T testBoxState(BlockState state, int x, int y, int z, ShapeCache world);

    protected abstract T combineFlag(T oldFlag, T newFlag);

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
                if (shape == FULL_CUBE || VoxelShapes.matchesAnywhere(boxShape, shape, BooleanBiFunction.AND)) {
                    maxFloorHeight = Math.max(maxFloorHeight, shape.getMax(Direction.Axis.Y));
                    if (maxFloorHeight == 1) {
                        return 0.0;
                    }
                }
            }
        }
        return maxFloorHeight;
    }

    private boolean testFloor(final double xOff, final double yOff, final double zOff, final ShapeCache world) {
        final int minX = MathHelper.floor(floorBox.minX + xOff);
        final int maxX = MathHelper.ceil(floorBox.maxX + xOff + 0.0000001) - 1;
        final int minY = MathHelper.floor(floorBox.minY + yOff) - 1;
        final int maxY = MathHelper.ceil(floorBox.maxY + yOff + 0.0000001) - 1;
        final int minZ = MathHelper.floor(floorBox.minZ + zOff);
        final int maxZ = MathHelper.ceil(floorBox.maxZ + zOff + 0.0000001) - 1;
        final VoxelShape shape = floorShape.offset(0, -(1 - MathHelper.fractionalPart(yOff)), 0);

        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    final BlockState state = world.getBlockState(x, y, z);
                    final T newFlag = testFloorState(state, x, y, z, world);
                    if (newFlag != null) {
                        if (flag != null) {
                            flag = combineFlag(flag, newFlag);
                        } else {
                            flag = newFlag;
                        }
                    }
                    final VoxelShape voxelShape = world.getCollisionShape(x, y, z);
                    if (voxelShape != EMPTY && !voxelShape.isEmpty()) {
                        if (voxelShape == FULL_CUBE) {
                            final double xBox = (double) x - xOff;
                            final double yBox = 1 + (double) y - yOff;
                            final double zBox = (double) z - zOff;
                            if (floorBox.intersects(xBox, yBox, zBox, xBox + 1.0D, yBox + 1.0D, zBox + 1.0D)) {
                                return true;
                            }
                        } else if (VoxelShapes.matchesAnywhere(shape, voxelShape, BooleanBiFunction.AND)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean testBox(final double xOff, final double yOff, final double zOff, final ShapeCache world) {
        final Box box = this.box;
        final VoxelShape shape = boxShape.offset(0, MathHelper.fractionalPart(yOff), zOff);
        final int minX = MathHelper.floor(box.minX + xOff);
        final int maxX = MathHelper.ceil(box.maxX + xOff + 0.0000001) - 1;
        final int minY = MathHelper.floor(box.minY + yOff);
        final int maxY = MathHelper.ceil(box.maxY + yOff + 0.0000001);
        final int minZ = MathHelper.floor(box.minZ + zOff);
        final int maxZ = MathHelper.ceil(box.maxZ + zOff + 0.0000001) - 1;

        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    final BlockState state = world.getBlockState(x, y, z);
                    final T newFlag = testBoxState(state, x, y, z, world);
                    if (newFlag != null) {
                        if (flag != null) {
                            flag = combineFlag(flag, newFlag);
                        } else {
                            flag = newFlag;
                        }
                    }
                    if (!state.isAir()) {
                        final VoxelShape voxelShape = world.getCollisionShape(x, y, z);
                        if (voxelShape != EMPTY && !voxelShape.isEmpty()) {
                            if (voxelShape == FULL_CUBE) {
                                final double xBox = (double) x - xOff;
                                final double yBox = (double) y - yOff;
                                final double zBox = (double) z - zOff;
                                if (box.intersects(xBox, yBox, zBox, xBox + 1.0D, yBox + 1.0D, zBox + 1.0D)) {
                                    return true;
                                }
                            } else if (VoxelShapes.matchesAnywhere(shape, voxelShape, BooleanBiFunction.AND)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    public FlaggedCollisionHelper(final double width, final double height, final R openFlag, final R closedFlag, final R floorFlag) {
        this.openFlag = openFlag;
        this.closedFlag = closedFlag;
        this.floorFlag = floorFlag;
        final double width2 = width * 0.5;
        box = new Box(0.5 - width2, 0.0D, 0.5 - width2, 0.5 + width2, height, 0.5 + width2);
        floorBox = new Box(0.5 - width2, 0.998, 0.5 - width2, 0.5 + width2, 0.999, 0.5 + width2);
        boxShape = VoxelShapes.cuboid(box);
        floorShape = VoxelShapes.cuboid(floorBox);
    }
}
