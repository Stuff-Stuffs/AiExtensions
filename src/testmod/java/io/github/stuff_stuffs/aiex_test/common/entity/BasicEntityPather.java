package io.github.stuff_stuffs.aiex_test.common.entity;

import io.github.stuff_stuffs.advanced_ai.common.api.util.AStar;
import io.github.stuff_stuffs.advanced_ai.common.api.util.CostGetter;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.AbstractEntityPather;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.BasicPathingUniverse;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.EntityPather;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class BasicEntityPather extends AbstractEntityPather<EntityPather.EntityContext, BasicEntityPather.BasicEntityNode> {
    public BasicEntityPather(final MobEntity entity) {
        super(entity);
    }

    @Override
    protected AStar<BasicEntityNode, EntityContext, Target> createPathfinder() {
        return new AStar<>(BasicEntityNode.class) {
            @Override
            protected double heuristic(final BasicEntityNode node, final Target target, final EntityContext context) {
                if (target instanceof SingleTarget single) {
                    final double yOff = (node.flags & BasicEntityNode.HALF_BLOCK_OR_MORE_FLAG) != 0 ? 0.65 : (node.flags & BasicEntityNode.LESS_THAN_HALF_BLOCK_FLAG) != 0 ? 0.25 : 0;
                    final double dx = Math.abs(node.x - single.target().x);
                    final double dy = Math.abs(node.y + yOff - single.target().y);
                    final double dz = Math.abs(node.z - single.target().z);
                    return (dx + dy + dz) * INV_ERROR_SCALE;
                } else if (target instanceof MetricTarget metric) {
                    return metric.score(node.x, node.y, node.z, context) * INV_ERROR_SCALE;
                }
                throw new AssertionError();
            }

            @Override
            protected double nodeCost(final BasicEntityNode node) {
                return node.cost;
            }

            @Override
            protected boolean shouldCutoff(final BasicEntityNode node, final EntityContext context) {
                return node.pathLength > context.maxPathLength();
            }

            @Override
            protected long key(final BasicEntityNode node) {
                return BlockPos.asLong(node.x, node.y, node.z);
            }

            @Override
            protected @Nullable BasicEntityNode previousNode(final BasicEntityNode node) {
                return node.previous;
            }

            private BasicEntityNode createJump(final int x, final int y, final int z, final BasicEntityNode prev, final ShapeCache shapeCache, final CostGetter costGetter) {
                final BasicPathingUniverse nodeType = getLocationType(x, y, z, shapeCache);
                return nodeType.floor && costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost + nodeType.costMultiplier ? new BasicEntityNode(x, y, z, prev, prev.cost + nodeType.costMultiplier, prev.pathLength + 1.5, 0, nodeType, getFlags(x, y, z, shapeCache), MovementType.WALK) : null;
            }

            private BasicEntityNode createAir(final int x, final int y, final int z, final BasicEntityNode prev, final ShapeCache shapeCache, final CostGetter costGetter) {
                final BasicPathingUniverse nodeType = getLocationType(x, y, z, shapeCache);
                return nodeType.air && costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost + nodeType.costMultiplier * 1.3 ? new BasicEntityNode(x, y, z, prev, prev.cost + nodeType.costMultiplier * 1.3, prev.pathLength + 1, nodeType.floor ? 0 : prev.fallBlocks + 1, nodeType, getFlags(x, y, z, shapeCache), MovementType.JUMP) : null;
            }

            private BasicEntityNode createBasic(final int x, final int y, final int z, final BasicEntityNode prev, final ShapeCache shapeCache, final CostGetter costGetter) {
                final BasicPathingUniverse nodeType = getLocationType(x, y, z, shapeCache);
                if (nodeType != BasicPathingUniverse.BLOCKED && costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost + nodeType.costMultiplier) {
                    return new BasicEntityNode(x, y, z, prev, prev.cost + nodeType.costMultiplier, prev.pathLength + 1, nodeType.floor ? 0 : prev.fallBlocks + 1, nodeType, getFlags(x, y, z, shapeCache), MovementType.WALK);
                }
                return null;
            }

            private BasicEntityNode createAuto(final int x, final int y, final int z, final BasicEntityNode prev, final ShapeCache shapeCache, final CostGetter costGetter) {
                final BasicPathingUniverse nodeType = getLocationType(x, y, z, shapeCache);
                if (nodeType != BasicPathingUniverse.BLOCKED && costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost + nodeType.costMultiplier) {
                    final boolean wasInAir = !prev.type.floor;
                    final boolean isInAir = !nodeType.floor;
                    return new BasicEntityNode(x, y, z, prev, prev.cost + (wasInAir & !isInAir && !nodeType.air ? prev.fallBlocks * 0.25 + nodeType.costMultiplier : nodeType.costMultiplier), prev.pathLength + 1, isInAir ? prev.fallBlocks + 1 : 0, nodeType, getFlags(x, y, z, shapeCache), isInAir ? MovementType.FALL : MovementType.WALK);
                } else {
                    return null;
                }
            }

            private BasicPathingUniverse getLocationType(final int x, final int y, final int z, final ShapeCache shapeCache) {
                return shapeCache.getLocationCache(x, y, z, BasicPathingUniverse.BLOCKED, BasicPathingUniverse.CLASSIFIER);
            }

            private int floorNodes(final BasicEntityNode previous, final ShapeCache cache, final CostGetter costGetter, final BasicEntityNode[] successors, int i, final int yOff) {
                BasicEntityNode node = createBasic(previous.x + 1, previous.y + yOff, previous.z, previous, cache, costGetter);
                if (node != null) {
                    successors[i++] = node;
                }

                node = createBasic(previous.x - 1, previous.y + yOff, previous.z, previous, cache, costGetter);
                if (node != null) {
                    successors[i++] = node;
                }

                node = createBasic(previous.x, previous.y + yOff, previous.z + 1, previous, cache, costGetter);
                if (node != null) {
                    successors[i++] = node;
                }

                node = createBasic(previous.x, previous.y + yOff, previous.z - 1, previous, cache, costGetter);
                if (node != null) {
                    successors[i++] = node;
                }
                return i;
            }

            @Override
            protected int neighbours(final BasicEntityNode previous, final EntityContext context, final CostGetter costGetter, final BasicEntityNode[] successors) {
                int i = 0;
                final ShapeCache cache = context.cache();
                BasicEntityNode node;
                if (previous.type.floor) {
                    i = floorNodes(previous, cache, costGetter, successors, i, 0);
                    if ((previous.flags & BasicEntityNode.HALF_BLOCK_OR_MORE_FLAG) != 0 && getLocationType(previous.x, previous.y + 1, previous.z, cache) != BasicPathingUniverse.BLOCKED) {
                        i = floorNodes(previous, cache, costGetter, successors, i, 1);
                    }
                } else if (previous.movementType == MovementType.JUMP) {
                    node = createJump(previous.x + 1, previous.y, previous.z, previous, cache, costGetter);
                    if (node != null) {
                        successors[i++] = node;
                    }

                    node = createJump(previous.x - 1, previous.y, previous.z, previous, cache, costGetter);
                    if (node != null) {
                        successors[i++] = node;
                    }

                    node = createJump(previous.x, previous.y, previous.z + 1, previous, cache, costGetter);
                    if (node != null) {
                        successors[i++] = node;
                    }

                    node = createJump(previous.x, previous.y, previous.z - 1, previous, cache, costGetter);
                    if (node != null) {
                        successors[i++] = node;
                    }
                }

                if (previous.type.floor) {
                    node = createAir(previous.x, previous.y + 1 + ((previous.flags & BasicEntityNode.HALF_BLOCK_OR_MORE_FLAG) != 0 ? 1 : 0), previous.z, previous, cache, costGetter);
                    if (node != null) {
                        successors[i++] = node;
                    }
                } else {
                    node = createAuto(previous.x, previous.y - 1, previous.z, previous, cache, costGetter);
                    if (node != null) {
                        successors[i++] = node;
                    }
                }
                return i;
            }
        };
    }

    @Override
    protected BasicEntityNode createCurrent(final ShapeCache cache) {
        final int x = entity.getBlockX();
        final int y = entity.getBlockY();
        final int z = entity.getBlockZ();
        final BasicPathingUniverse type = BasicPathingUniverse.CLASSIFIER.get(x, y, z, cache);
        return new BasicEntityNode(x, y, z, null, 0, 0, 0, type, getFlags(x, y, z, cache), type.floor ? MovementType.WALK : MovementType.JUMP);
    }

    @Override
    protected EntityContext createContext(final ShapeCache cache, final double error, final double maxPathLength, final boolean partial, final double urgency) {
        return new EntityContext() {
            @Override
            public Entity entity() {
                return entity;
            }

            @Override
            public double maxPathLength() {
                return maxPathLength;
            }

            @Override
            public ShapeCache cache() {
                return cache;
            }
        };
    }

    @Override
    protected BlockPos fromNode(final BasicEntityNode node) {
        return new BlockPos(node.x, node.y, node.z);
    }

    public static final class BasicEntityNode {
        public static final long LESS_THAN_HALF_BLOCK_FLAG = 0x1;
        public static final long HALF_BLOCK_OR_MORE_FLAG = 0x2;
        private final int x;
        private final int y;
        private final int z;
        private final BasicEntityNode previous;
        private final double cost;
        private final double pathLength;
        private final int fallBlocks;
        private final BasicPathingUniverse type;
        private final long flags;
        private final MovementType movementType;

        public BasicEntityNode(
                final int x,
                final int y,
                final int z,
                final BasicEntityNode previous,
                final double cost,
                final double pathLength,
                final int fallBlocks,
                final BasicPathingUniverse type,
                final long flags,
                final MovementType movementType
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.previous = previous;
            this.cost = cost;
            this.pathLength = pathLength;
            this.fallBlocks = fallBlocks;
            this.type = type;
            this.flags = flags;
            this.movementType = movementType;
        }

        @Override
        public String toString() {
            return "BasicEntityNode[" +
                    "x=" + x + ", " +
                    "y=" + y + ", " +
                    "z=" + z + ", " +
                    "cost=" + cost + ", " +
                    "pathLength=" + pathLength + ", " +
                    "fallBlocks=" + fallBlocks + ", " +
                    "type=" + type + ", " +
                    "flags=" + flags + ", " +
                    "movementType=" + movementType + ']';
        }
    }

    public static long getFlags(final int x, final int y, final int z, final ShapeCache cache) {
        final double max = cache.getCollisionShape(x, y, z).getMax(Direction.Axis.Y);
        long flags = 0;
        if (max > 0) {
            if (max >= 0.5) {
                flags |= BasicEntityNode.HALF_BLOCK_OR_MORE_FLAG;
            } else {
                flags |= BasicEntityNode.LESS_THAN_HALF_BLOCK_FLAG;
            }
        }
        return flags;
    }

    public enum MovementType {
        WALK,
        JUMP,
        FALL
    }
}
