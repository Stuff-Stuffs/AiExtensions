package io.github.stuff_stuffs.aiex.common.api.entity.movement;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class DoorUtil {
    public static boolean canPassThroughDoor(final BlockState state, final Direction going) {
        final Direction facing = state.getOrEmpty(DoorBlock.FACING).orElse(Direction.NORTH);
        if (facing.getAxis() == Direction.Axis.Y) {
            return true;
        }
        Direction blockedClosed = facing;
        if (state.getOrEmpty(DoorBlock.OPEN).orElse(false)) {
            final DoorHinge hinge = state.getOrEmpty(DoorBlock.HINGE).orElse(DoorHinge.LEFT);
            if (hinge == DoorHinge.LEFT) {
                blockedClosed = blockedClosed.rotateClockwise(Direction.Axis.Y);
            } else {
                blockedClosed = blockedClosed.rotateCounterclockwise(Direction.Axis.Y);
            }
        }
        return blockedClosed.getAxis() != going.getAxis();
    }

    public static @Nullable BlockHitResult raycastDoor(final BlockState state, final World world, final BlockPos pos, final Entity entity) {
        final Vec3d offset = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
        final VoxelShape shape = state.getOutlineShape(world, pos);
        if (shape.isEmpty()) {
            return null;
        }
        final Vec3d point = shape.getClosestPointTo(entity.getPos().subtract(offset)).orElseThrow(AssertionError::new).add(offset);
        final Vec3d start = entity.getEyePos();
        final Vec3d delta = point.subtract(start);
        final Vec3d end = point.add(delta.multiply(0.01));
        final BlockHitResult raycast = shape.raycast(start, end, pos);
        if (raycast == null) {
            return new BlockHitResult(point, Direction.NORTH, pos, false);
        }
        return raycast;
    }

    private DoorUtil() {
    }
}
