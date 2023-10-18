package io.github.stuff_stuffs.aiex_test.common.entity;

import io.github.stuff_stuffs.advanced_ai.common.api.util.CollisionHelper;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.internal.AdvancedAiPathing;
import io.github.stuff_stuffs.advanced_ai.common.internal.pathing.AStar;
import io.github.stuff_stuffs.advanced_ai.common.internal.pathing.CostGetter;
import io.github.stuff_stuffs.aiex.common.api.entity.EntityPather;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BasicEntityNavigator implements EntityPather {
    private final MobEntity entity;
    private final AStar<AiExTestEntities.BasicEntityNode, EntityContext, Target> pathfinder;
    private @Nullable EntityPather.Target target = null;
    private double lastMaxError = 0.0;
    private double lastMaxCost = 0.0;
    private boolean lastPartial;
    private @Nullable List<Wrapper> currentPath = null;
    private int currentIndex = 0;
    private double urgency = 0.0;

    public BasicEntityNavigator(final MobEntity entity) {
        this.entity = entity;
        pathfinder = createPathFinder();
    }

    private AiExTestEntities.BasicEntityNode createCurrent() {
        return new AiExTestEntities.BasicEntityNode(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ(), null, 0, entity.isOnGround());
    }

    @Override
    public boolean startFollowingPath(final Target target, final double error, final double maxCost, final boolean partial, final double urgency) {
        final BlockPos pos = entity.getBlockPos();
        final ShapeCache cache = ShapeCache.create(entity.getEntityWorld(), pos.add(-64, -64, -64), pos.add(64, 64, 64), 4096);
        final AStar.PathInfo<AiExTestEntities.BasicEntityNode> path = pathfinder.findPath(createCurrent(), new EntityContext() {
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
        for (final AiExTestEntities.BasicEntityNode node : path.path()) {
            currentPath.add(new Wrapper(node));
        }
        currentIndex = 0;
        lastMaxCost = maxCost;
        lastMaxError = error;
        lastPartial = partial;
        currentPath.get(0).timeoutTimer = 20;
        this.urgency = urgency;
        return true;
    }

    public AStar<AiExTestEntities.BasicEntityNode, EntityContext, Target> createPathFinder() {
        return new AStar<>(AiExTestEntities.BasicEntityNode.class) {
            @Override
            protected double heuristic(final AiExTestEntities.BasicEntityNode node, final Target target, final EntityContext context) {
                if (target instanceof SingleTarget single) {
                    return Math.sqrt(single.target().squaredDistanceTo(node.x() + 0.5, node.y(), node.z() + 0.5));
                } else if (target instanceof MetricTarget metric) {
                    return metric.score(node.x(), node.y(), node.z(), context);
                }
                throw new AssertionError();
            }

            @Override
            protected double nodeCost(final AiExTestEntities.BasicEntityNode node) {
                return node.cost();
            }

            @Override
            protected long key(final AiExTestEntities.BasicEntityNode node) {
                return BlockPos.asLong(node.x(), node.y(), node.z());
            }

            @Override
            protected @Nullable AiExTestEntities.BasicEntityNode previousNode(final AiExTestEntities.BasicEntityNode node) {
                return node.previous();
            }

            private AiExTestEntities.BasicEntityNode createJump(final int x, final int y, final int z, final AiExTestEntities.BasicEntityNode prev, final ShapeCache shapeCache, final CostGetter costGetter) {
                return costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost() + 1.0D && getLocationType(x, y, z, shapeCache) == CollisionHelper.FloorCollision.FLOOR ? new AiExTestEntities.BasicEntityNode(x, y, z, prev, prev.cost() + 1.0D, true) : null;
            }

            private AiExTestEntities.BasicEntityNode createAir(final int x, final int y, final int z, final AiExTestEntities.BasicEntityNode prev, final ShapeCache shapeCache, final CostGetter costGetter) {
                return costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost() + 1.0D && getLocationType(x, y, z, shapeCache) == CollisionHelper.FloorCollision.OPEN ? new AiExTestEntities.BasicEntityNode(x, y, z, prev, prev.cost() + 1.0D, false) : null;
            }

            private AiExTestEntities.BasicEntityNode createBasic(final int x, final int y, final int z, final AiExTestEntities.BasicEntityNode prev, final ShapeCache shapeCache, final CostGetter costGetter) {
                final CollisionHelper.FloorCollision collision;
                return costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost() + 1.0D && (collision = getLocationType(x, y, z, shapeCache)) != CollisionHelper.FloorCollision.CLOSED ? new AiExTestEntities.BasicEntityNode(x, y, z, prev, prev.cost() + 1.0D, collision == CollisionHelper.FloorCollision.FLOOR) : null;
            }

            private AiExTestEntities.BasicEntityNode createAuto(final int x, final int y, final int z, final AiExTestEntities.BasicEntityNode prev, final ShapeCache shapeCache, final CostGetter costGetter) {
                final CollisionHelper.FloorCollision type;
                if (costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost() + 1.0D && (type = getLocationType(x, y, z, shapeCache)) != CollisionHelper.FloorCollision.CLOSED) {
                    final boolean ground = type == CollisionHelper.FloorCollision.FLOOR;
                    return new AiExTestEntities.BasicEntityNode(x, y, z, prev, prev.cost() + (double) (ground ? 10 : 1), ground);
                } else {
                    return null;
                }
            }

            private CollisionHelper.FloorCollision getLocationType(final int x, final int y, final int z, final ShapeCache shapeCache) {
                return shapeCache.getLocationCache(x, y, z, CollisionHelper.FloorCollision.CLOSED, AdvancedAiPathing.BASIC);
            }

            @Override
            protected int neighbours(final AiExTestEntities.BasicEntityNode previous, final EntityContext context, final CostGetter costGetter, final AiExTestEntities.BasicEntityNode[] successors) {
                int i = 0;
                final ShapeCache cache = context.cache();
                AiExTestEntities.BasicEntityNode node;
                if (previous.onGround()) {
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
                }

                if (!previous.onGround() && previous.previous()!=null && previous.previous().onGround()) {
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

                if (previous.onGround()) {
                    node = createAir(previous.x(), previous.y() + 1, previous.z(), previous, cache, costGetter);
                    if (node != null) {
                        successors[i++] = node;
                    }
                }

                if (!previous.onGround()) {
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
                currentPath = null;
                currentIndex = 0;
                startFollowingPath(target, lastMaxError, lastMaxCost, lastPartial, urgency);
            }
        }
    }

    private void continueAlongPath() {
        assert currentPath != null;
        final double nodeReachProximity = (entity.getWidth() > 0.75F ? entity.getWidth() / 2.0F : 0.75F - entity.getWidth() / 2.0F);
        final AiExTestEntities.BasicEntityNode vec3i = currentPath.get(currentIndex).node;
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
            currentPath.get(currentIndex).timeoutTimer = 20;
        }
        final Wrapper wrapper = currentPath.get(currentIndex);
        double offX = (entity.getWidth() + 1.0F) * 0.5;
        double offZ = (entity.getWidth() + 1.0F) * 0.5;
        final Vec3d vec = new Vec3d(wrapper.node.x() + offX, wrapper.node.y(), wrapper.node.z() + offZ);
        entity.getMoveControl().moveTo(wrapper.node.x() + offX, adjustTargetY(vec), wrapper.node.z() + offZ, 0.6);
    }

    private double adjustTargetY(final Vec3d pos) {
        final BlockPos blockPos = BlockPos.ofFloored(pos);
        final World world = entity.getEntityWorld();
        return world.getBlockState(blockPos.down()).isAir() ? pos.y : LandPathNodeMaker.getFeetY(world, blockPos);
    }

    private static final class Wrapper {
        private final AiExTestEntities.BasicEntityNode node;
        private int timeoutTimer = 0;

        private Wrapper(final AiExTestEntities.BasicEntityNode node) {
            this.node = node;
        }
    }
}
