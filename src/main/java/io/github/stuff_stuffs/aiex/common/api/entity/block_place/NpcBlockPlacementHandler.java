package io.github.stuff_stuffs.aiex.common.api.entity.block_place;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Set;
import java.util.function.Predicate;

public interface NpcBlockPlacementHandler<T> {
    boolean handle(Item item, BrainContext<T> context, BlockPos pos, Predicate<BlockState> targetState);

    Set<Item> handles();

    static BlockHitResult castAt(final BlockPos pos, final Direction side, final double y, final double horizontal) {
        final Vec3d center = Vec3d.ofCenter(pos);
        final double dir = side.getDirection() == Direction.AxisDirection.POSITIVE ? -1.0 : 1.0;
        final Direction.Axis axis = side.getAxis() == Direction.Axis.Y ? Direction.Axis.Z : side.rotateYClockwise().getAxis();
        final Vec3d hitPos = center.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5).add(axis.choose(0, 0, (horizontal - 0.5) * dir), y - 0.5, axis.choose((horizontal - 0.5) * dir, 0, 0));
        return new BlockHitResult(hitPos, side, pos.toImmutable(), false);
    }
}
