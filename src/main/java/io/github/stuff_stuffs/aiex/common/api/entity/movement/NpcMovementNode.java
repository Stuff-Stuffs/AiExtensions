package io.github.stuff_stuffs.aiex.common.api.entity.movement;

import io.github.stuff_stuffs.aiex.common.api.entity.AbstractNpcEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public sealed interface NpcMovementNode {
    double x();

    double y();

    double z();

    default int ticksTillFailure(final AbstractNpcEntity entity) {
        return 40;
    }

    default boolean reached(final AbstractNpcEntity entity, @Nullable final NpcMovementNode next) {
        final World world = entity.getEntityWorld();
        final BlockPos pos = BlockPos.ofFloored(x(), y(), z());
        double y = y();
        if (entity.getY() < y) {
            y = y + Math.min(Math.max(world.getBlockState(pos).getCollisionShape(world, pos).getMax(Direction.Axis.Y), 0), 2);
        }
        final double distSq = entity.getPos().squaredDistanceTo(x(), y, z());
        if (distSq < 0.375 * 0.375) {
            return true;
        }
        if (next == null) {
            return false;
        }
        final Vec3d delta = new Vec3d(next.x() - x(), next.y() - y, next.z() - z()).normalize();
        final Vec3d translated = entity.getPos().subtract(x(), y, z());
        return distSq < 1 && delta.dotProduct(translated) > 0.1;
    }

    record OpenDoor(NpcMovementNode inner, Direction direction, int doorX, int doorY,
                    int doorZ) implements NpcMovementNode {
        @Override
        public double x() {
            return inner.x();
        }

        @Override
        public double y() {
            return inner.y();
        }

        @Override
        public double z() {
            return inner.z();
        }

        @Override
        public int ticksTillFailure(final AbstractNpcEntity entity) {
            return inner.ticksTillFailure(entity);
        }

        @Override
        public boolean reached(final AbstractNpcEntity entity, @Nullable final NpcMovementNode next) {
            return inner.reached(entity, next);
        }
    }

    record Walk(double x, double y, double z) implements NpcMovementNode {
    }

    record Jump(double x, double y, double z) implements NpcMovementNode {
    }

    record Fall(double x, double y, double z) implements NpcMovementNode {
        @Override
        public boolean reached(final AbstractNpcEntity entity, @Nullable final NpcMovementNode next) {
            if (entity.getY() + 0.25 < y) {
                final double dx = entity.getX() - x();
                final double dz = entity.getZ() - z();
                return dx * dx + dz * dz < 0.375 * 0.375;
            }
            return entity.getPos().squaredDistanceTo(x(), y(), z()) < 0.375 * 0.375;
        }
    }

    record ClimbLadder(double x, double y, double z) implements NpcMovementNode {
    }
}
