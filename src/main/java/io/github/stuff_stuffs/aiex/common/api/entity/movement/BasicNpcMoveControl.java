package io.github.stuff_stuffs.aiex.common.api.entity.movement;

import io.github.stuff_stuffs.aiex.common.api.entity.AbstractNpcEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BasicNpcMoveControl implements NpcMoveControl {
    private final AbstractNpcEntity entity;
    private boolean failed = false;
    private @Nullable Wrapper[] nodes;
    private int index = 0;

    public BasicNpcMoveControl(final AbstractNpcEntity entity) {
        this.entity = entity;
    }

    @Override
    public void set(final List<NpcMovementNode> nodes) {
        final int size = nodes.size();
        this.nodes = new Wrapper[size];
        for (int i = 0; i < size; i++) {
            final NpcMovementNode node = nodes.get(i);
            this.nodes[i] = new Wrapper(node, node.ticksTillFailure(entity));
        }
        failed = false;
        index = 0;
    }

    @Override
    public boolean failedLastPath() {
        return failed;
    }

    @Override
    public void tick() {
        if (nodes == null || index >= nodes.length) {
            entity.setMovementSpeed(0.0F);
            nodes = null;
            index = 0;
            return;
        }
        final Wrapper wrapper = nodes[index];
        final NpcMovementNode node = wrapper.node;
        handle(node);
        if (node.reached(entity, index + 1 == nodes.length ? null : nodes[index + 1].node)) {
            if (index - 1 >= 0) {
                handleLast(nodes[index - 1].node);
            }
            index++;
        } else if (wrapper.ticksTillFailure-- < 0) {
            entity.setMovementSpeed(0.0F);
            nodes = null;
            index = 0;
            failed = true;
        }
    }

    protected void handleLast(final NpcMovementNode node) {
        if (node instanceof NpcMovementNode.OpenDoor door) {
            closeDoor(door);
        }
    }

    protected void handle(final NpcMovementNode node) {
        if (node instanceof NpcMovementNode.Walk walk) {
            walk(walk);
        } else if (node instanceof NpcMovementNode.Jump jump) {
            jump(jump);
        } else if (node instanceof NpcMovementNode.Fall fall) {
            fall(fall);
        } else if (node instanceof NpcMovementNode.ClimbLadder ladder) {
            climbLadder(ladder);
        } else if (node instanceof NpcMovementNode.OpenDoor door) {
            openDoor(door);
        }
    }

    @Override
    public boolean idle() {
        return nodes == null || index >= nodes.length;
    }

    protected void moveTo(final double x, final double y, final double z) {
        final double dx = x - entity.getX();
        final World world = entity.getEntityWorld();
        final BlockPos pos = BlockPos.ofFloored(x, y, z);
        final double dy = (y + Math.min(Math.max(world.getBlockState(pos).getCollisionShape(world, pos).getMax(Direction.Axis.Y), 0), 2) + world.getBlockState(pos).getCollisionShape(world, pos).getMax(Direction.Axis.Y)) - entity.getY();
        final double dz = z - entity.getZ();
        final double distSq = dx * dx + dz * dz + dy * dy;
        if (distSq < 0.0001) {
            entity.setForwardSpeed(0.0F);
            return;
        }

        final float angle = (float) (MathHelper.atan2(dz, dx) * 180.0F / (float) Math.PI) - 90.0F;
        entity.setYaw(wrapDegrees(entity.getYaw(), angle, 90.0F));
        final float speed = (float) Math.min(entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * 0.35, Math.sqrt(distSq) * 0.95);
        entity.setMovementSpeed(speed);
    }

    protected void walk(final NpcMovementNode.Walk walk) {
        moveTo(walk.x(), walk.y(), walk.z());
    }

    protected void jump(final NpcMovementNode.Jump jump) {
        moveTo(jump.x(), jump.y(), jump.z());
        final double dy = jump.y() - entity.getY();
        final double dx = jump.x() - entity.getX();
        final double dz = jump.z() - entity.getZ();
        if (dy > 0.001 || (dx * dx + dz * dz) > 0.5 * 0.5) {
            entity.getJumpControl().setActive();
        }
    }

    protected void fall(final NpcMovementNode.Fall fall) {
        moveTo(fall.x(), fall.y(), fall.z());
    }

    protected void climbLadder(final NpcMovementNode.ClimbLadder ladder) {
        final double dx = ladder.x() - entity.getX();
        final double dz = ladder.z() - entity.getZ();
        final double distSq = dx * dx + dz * dz;
        if (distSq > 0.1 * 0.1) {
            moveTo(ladder.x(), ladder.y(), ladder.z());
        }
        final double dy = ladder.y() - entity.getY();
        if (dy > 0.1) {
            entity.getJumpControl().setActive();
        }
    }

    protected void closeDoor(final NpcMovementNode.OpenDoor door) {
        final World world = entity.getEntityWorld();
        final BlockPos pos = BlockPos.ofFloored(door.doorX(), door.doorY(), door.doorZ());
        final Direction direction = door.direction();
        final BlockState state = world.getBlockState(pos);
        if (DoorUtil.canPassThroughDoor(state, direction) && entity.aiex$getBrain().hasFakePlayerDelegate()) {
            final BlockHitResult target = DoorUtil.raycastDoor(state, world, pos, entity);
            if (target != null) {
                final ActionResult result = state.onUse(world, entity.aiex$getBrain().fakePlayerDelegate(), Hand.MAIN_HAND, target);
                if (result.shouldSwingHand()) {
                    entity.swingHand(Hand.MAIN_HAND);
                }
            }
        }
    }

    protected void openDoor(final NpcMovementNode.OpenDoor door) {
        final World world = entity.getEntityWorld();
        final BlockPos pos = BlockPos.ofFloored(door.doorX(), door.doorY(), door.doorZ());
        final Direction direction = door.direction();
        final BlockState state = world.getBlockState(pos);
        if (!DoorUtil.canPassThroughDoor(state, direction) && entity.aiex$getBrain().hasFakePlayerDelegate()) {
            final BlockHitResult target = DoorUtil.raycastDoor(state, world, pos, entity);
            if (target != null) {
                final ActionResult result = state.onUse(world, entity.aiex$getBrain().fakePlayerDelegate(), Hand.MAIN_HAND, target);
                if (result.shouldSwingHand()) {
                    entity.swingHand(Hand.MAIN_HAND);
                }
            }
        }
        handle(door.inner());
    }

    protected float wrapDegrees(final float from, final float to, final float max) {
        float f = MathHelper.wrapDegrees(to - from);
        if (f > max) {
            f = max;
        }

        if (f < -max) {
            f = -max;
        }

        float g = from + f;
        if (g < 0.0F) {
            g += 360.0F;
        } else if (g > 360.0F) {
            g -= 360.0F;
        }

        return g;
    }

    private static final class Wrapper {
        private final NpcMovementNode node;
        private int ticksTillFailure;

        private Wrapper(final NpcMovementNode node, final int ticksTillFailure) {
            this.node = node;
            this.ticksTillFailure = ticksTillFailure;
        }
    }
}
