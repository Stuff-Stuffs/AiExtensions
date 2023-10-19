package io.github.stuff_stuffs.aiex_test.common.entity;

import io.github.stuff_stuffs.advanced_ai.common.api.util.AStar;
import io.github.stuff_stuffs.advanced_ai.common.api.util.CostGetter;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.aiex.common.api.entity.EntityPather;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.BasicPathingUniverse;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BasicEntityPather implements EntityPather {
    private final MobEntity entity;
    private final AStar<BasicEntityNode, EntityContext, Target> pathfinder;
    private @Nullable EntityPather.Target target = null;
    private double lastMaxError = 0.0;
    private double lastMaxCost = 0.0;
    private boolean lastPartial;
    private @Nullable List<Wrapper> currentPath = null;
    private int currentIndex = 0;
    private double urgency = 0.0;

    public BasicEntityPather(final MobEntity entity) {
        this.entity = entity;
        pathfinder = createPathFinder();
    }

    private BasicEntityNode createCurrent(final ShapeCache cache) {
        final int x = entity.getBlockX();
        final int y = entity.getBlockY();
        final int z = entity.getBlockZ();
        final BasicPathingUniverse type = BasicPathingUniverse.CLASSIFIER.get(x, y, z, cache);
        return new BasicEntityNode(x, y, z, null, 0, 0, type, type.floor ? MovementType.WALK : MovementType.JUMP);
    }

    @Override
    public boolean startFollowingPath(final Target target, final double error, final double maxCost, final boolean partial, final double urgency) {
        final BlockPos pos = entity.getBlockPos();
        final ShapeCache cache = ShapeCache.create(entity.getEntityWorld(), pos.add(-64, -64, -64), pos.add(64, 64, 64), 4096);
        final AStar.PathInfo<BasicEntityNode> path = pathfinder.findPath(createCurrent(cache), new EntityContext() {
            @Override
            public Entity entity() {
                return entity;
            }

            @Override
            public ShapeCache cache() {
                return cache;
            }
        }, target, error, maxCost, partial);
        if (path == null || path.path() == null) {
            currentPath = null;
            this.urgency = 0;
            this.target = null;
            return false;
        }
        this.target = target;
        currentPath = new ArrayList<>(path.path().size());
        for (final BasicEntityNode node : path.path()) {
            currentPath.add(new Wrapper(node));
        }
        currentIndex = 0;
        lastMaxCost = maxCost;
        lastMaxError = error;
        lastPartial = partial;
        currentPath.get(0).timeoutTimer = 40;
        this.urgency = urgency;
        return true;
    }

    public AStar<BasicEntityNode, EntityContext, Target> createPathFinder() {
        return new AStar<>(BasicEntityNode.class) {
            @Override
            protected double heuristic(final BasicEntityNode node, final Target target, final EntityContext context) {
                if (target instanceof SingleTarget single) {
                    final double dx = Math.abs(node.x() - single.target().x);
                    final double dy = Math.abs(node.y() - single.target().y);
                    final double dz = Math.abs(node.z() - single.target().z);
                    return dx + dy + dz;
                } else if (target instanceof MetricTarget metric) {
                    return metric.score(node.x(), node.y(), node.z(), context);
                }
                throw new AssertionError();
            }

            @Override
            protected double nodeCost(final BasicEntityNode node) {
                return node.cost();
            }

            @Override
            protected long key(final BasicEntityNode node) {
                return BlockPos.asLong(node.x(), node.y(), node.z());
            }

            @Override
            protected @Nullable BasicEntityNode previousNode(final BasicEntityNode node) {
                return node.previous();
            }

            private BasicEntityNode createJump(final int x, final int y, final int z, final BasicEntityNode prev, final ShapeCache shapeCache, final CostGetter costGetter) {
                final BasicPathingUniverse nodeType = getLocationType(x, y, z, shapeCache);
                return nodeType.floor && costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost() + nodeType.costMultiplier ? new BasicEntityNode(x, y, z, prev, prev.cost() + nodeType.costMultiplier, 0, nodeType, MovementType.WALK) : null;
            }

            private BasicEntityNode createAir(final int x, final int y, final int z, final BasicEntityNode prev, final ShapeCache shapeCache, final CostGetter costGetter) {
                final BasicPathingUniverse nodeType = getLocationType(x, y, z, shapeCache);
                return nodeType.air && costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost() + nodeType.costMultiplier * 1.3 ? new BasicEntityNode(x, y, z, prev, prev.cost() + nodeType.costMultiplier * 1.3, nodeType.floor ? 0 : prev.fallBlocks() + 1, nodeType, MovementType.JUMP) : null;
            }

            private BasicEntityNode createBasic(final int x, final int y, final int z, final BasicEntityNode prev, final ShapeCache shapeCache, final CostGetter costGetter) {
                final BasicPathingUniverse nodeType = getLocationType(x, y, z, shapeCache);
                return nodeType != BasicPathingUniverse.BLOCKED && costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost() + nodeType.costMultiplier ? new BasicEntityNode(x, y, z, prev, prev.cost() + nodeType.costMultiplier, nodeType.floor ? 0 : prev.fallBlocks() + 1, nodeType, MovementType.WALK) : null;
            }

            private BasicEntityNode createAuto(final int x, final int y, final int z, final BasicEntityNode prev, final ShapeCache shapeCache, final CostGetter costGetter) {
                final BasicPathingUniverse nodeType = getLocationType(x, y, z, shapeCache);
                if (nodeType != BasicPathingUniverse.BLOCKED && costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost() + nodeType.costMultiplier) {
                    final boolean wasInAir = !prev.type().floor;
                    final boolean isInAir = !nodeType.floor;
                    return new BasicEntityNode(x, y, z, prev, prev.cost() + (wasInAir & !isInAir && !nodeType.air ? prev.fallBlocks() * 0.25 + nodeType.costMultiplier : nodeType.costMultiplier), isInAir ? prev.fallBlocks() + 1 : 0, nodeType, isInAir ? MovementType.FALL : MovementType.WALK);
                } else {
                    return null;
                }
            }

            private BasicPathingUniverse getLocationType(final int x, final int y, final int z, final ShapeCache shapeCache) {
                return shapeCache.getLocationCache(x, y, z, BasicPathingUniverse.BLOCKED, BasicPathingUniverse.CLASSIFIER);
            }

            @Override
            protected int neighbours(final BasicEntityNode previous, final EntityContext context, final CostGetter costGetter, final BasicEntityNode[] successors) {
                int i = 0;
                final ShapeCache cache = context.cache();
                BasicEntityNode node;
                if (previous.type().floor) {
                    node = createBasic(previous.x() + 1, previous.y(), previous.z(), previous, cache, costGetter);
                    if (node != null) {
                        successors[i++] = node;
                    }

                    node = createBasic(previous.x() - 1, previous.y(), previous.z(), previous, cache, costGetter);
                    if (node != null) {
                        successors[i++] = node;
                    }

                    node = createBasic(previous.x(), previous.y(), previous.z() + 1, previous, cache, costGetter);
                    if (node != null) {
                        successors[i++] = node;
                    }

                    node = createBasic(previous.x(), previous.y(), previous.z() - 1, previous, cache, costGetter);
                    if (node != null) {
                        successors[i++] = node;
                    }
                } else if (previous.movementType == MovementType.JUMP) {
                    node = createJump(previous.x() + 1, previous.y(), previous.z(), previous, cache, costGetter);
                    if (node != null) {
                        successors[i++] = node;
                    }

                    node = createJump(previous.x() - 1, previous.y(), previous.z(), previous, cache, costGetter);
                    if (node != null) {
                        successors[i++] = node;
                    }

                    node = createJump(previous.x(), previous.y(), previous.z() + 1, previous, cache, costGetter);
                    if (node != null) {
                        successors[i++] = node;
                    }

                    node = createJump(previous.x(), previous.y(), previous.z() - 1, previous, cache, costGetter);
                    if (node != null) {
                        successors[i++] = node;
                    }
                }

                if (previous.type().floor) {
                    node = createAir(previous.x(), previous.y() + 1, previous.z(), previous, cache, costGetter);
                    if (node != null) {
                        successors[i++] = node;
                    }
                } else {
                    node = createAuto(previous.x(), previous.y() - 1, previous.z(), previous, cache, costGetter);
                    if (node != null) {
                        successors[i++] = node;
                    }
                }

                return i;
            }
        };
    }

    @Override
    public void setUrgency(final double urgency) {
        this.urgency = urgency;
    }

    @Override
    public double urgency() {
        return urgency;
    }

    @Override
    public boolean idle() {
        return currentPath == null;
    }

    public void tick() {
        if (currentPath != null) {
            continueAlongPath();
            if (currentPath != null && (currentPath.get(currentIndex).timeoutTimer--) == 0) {
                System.out.println("Retry");
                currentPath = null;
                currentIndex = 0;
                startFollowingPath(target, lastMaxError, lastMaxCost, lastPartial, urgency);
            }
        }
    }

    private void continueAlongPath() {
        assert currentPath != null;
        final double nodeReachProximity = (entity.getWidth() > 0.75F ? entity.getWidth() / 2.0F : 0.75F - entity.getWidth() / 2.0F);
        final BasicEntityNode vec3i = currentPath.get(currentIndex).node;
        final double d = Math.abs(entity.getX() - (vec3i.x() + 0.5));
        final double e = Math.abs(entity.getY() - vec3i.y());
        final double f = Math.abs(entity.getZ() - (vec3i.z() + 0.5));
        final boolean reached = d < nodeReachProximity && f < nodeReachProximity && e < 1.0;
        if (reached) {
            currentIndex++;
            if (currentPath.size() == currentIndex) {
                currentPath = null;
                return;
            }
            currentPath.get(currentIndex).timeoutTimer = 40;
        }
        final Wrapper wrapper = currentPath.get(currentIndex);
        final double centerizer = (entity.getWidth() + 1 - MathHelper.fractionalPart(entity.getWidth())) * 0.5;
        final double x = wrapper.node.x() + centerizer;
        final double y = adjustTargetY(new BlockPos(wrapper.node.x, wrapper.node.y, wrapper.node.z));
        final double z = wrapper.node.z() + centerizer;
        entity.getMoveControl().moveTo(x, y, z, Math.min(entity.getPos().distanceTo(new Vec3d(x, y, z)) + 0.05, 0.4));
    }

    private double adjustTargetY(final BlockPos pos) {
        final World world = entity.getEntityWorld();
        final BlockState state = world.getBlockState(pos);
        return state.isAir() ? pos.getY() : pos.getY() + state.getCollisionShape(world, pos).getMax(Direction.Axis.Y);
    }

    private static final class Wrapper {
        private final BasicEntityNode node;
        private int timeoutTimer = 0;

        private Wrapper(final BasicEntityNode node) {
            this.node = node;
        }
    }

    public record BasicEntityNode(
            int x,
            int y,
            int z,
            BasicEntityNode previous,
            double cost,
            int fallBlocks,
            BasicPathingUniverse type,
            MovementType movementType
    ) {
    }

    public enum MovementType {
        WALK,
        JUMP,
        FALL
    }
}
