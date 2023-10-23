package io.github.stuff_stuffs.aiex.common.api.entity.pathing;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.AStar;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.CostGetter;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.ShapeCache;
import io.github.stuff_stuffs.aiex.common.api.entity.AbstractNpcEntity;
import io.github.stuff_stuffs.aiex.common.api.entity.movement.NpcMovementNode;
import io.github.stuff_stuffs.aiex.common.api.util.AiExMathUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BasicNpcEntityPather extends AbstractNpcEntityPather<EntityPather.EntityContext, BasicNpcEntityPather.BasicEntityNode> {
    public BasicNpcEntityPather(final AbstractNpcEntity entity) {
        super(entity);
    }

    @Override
    protected AStar<BasicEntityNode, EntityContext, Target> createPathfinder() {
        return new AStar<>(BasicEntityNode.class) {
            @Override
            protected boolean validEnd(final BasicEntityNode node, final EntityContext context) {
                return node.type.floor;
            }

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
                if (nodeType.floor && costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost + nodeType.costMultiplier) {
                    return new BasicEntityNode(x, y, z, prev, prev.cost + nodeType.costMultiplier, prev.pathLength + 1.5, 0, nodeType, getFlags(x, y, z, shapeCache), EntityMovementType.WALK, prev.state);
                }
                return null;
            }

            private BasicEntityNode createAir(final int x, final int y, final int z, final BasicEntityNode prev, final ShapeCache shapeCache, final CostGetter costGetter) {
                final BasicPathingUniverse nodeType = getLocationType(x, y, z, shapeCache);
                final EntityMovementType movementType = nodeType == BasicPathingUniverse.LADDER ? EntityMovementType.LADDER_CLIMB : nodeType.floor ? EntityMovementType.WALK : EntityMovementType.JUMP;
                if (nodeType.air && costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost + nodeType.costMultiplier * 1.3) {
                    return new BasicEntityNode(x, y, z, prev, prev.cost + nodeType.costMultiplier * 1.3, prev.pathLength + 1, nodeType.floor ? 0 : prev.fallBlocks + 1, nodeType, getFlags(x, y, z, shapeCache), movementType, prev.state);
                }
                return null;
            }

            private BasicEntityNode createBasic(final int x, final int y, final int z, final BasicEntityNode prev, final ShapeCache shapeCache, final CostGetter costGetter) {
                final BasicPathingUniverse nodeType = getLocationType(x, y, z, shapeCache);
                if (nodeType != BasicPathingUniverse.BLOCKED && costGetter.cost(BlockPos.asLong(x, y, z)) > prev.cost + nodeType.costMultiplier) {
                    return new BasicEntityNode(x, y, z, prev, prev.cost + nodeType.costMultiplier, prev.pathLength + 1, nodeType.floor ? 0 : prev.fallBlocks + 1, nodeType, getFlags(x, y, z, shapeCache), EntityMovementType.WALK, prev.state);
                }
                return null;
            }

            private BasicEntityNode createAuto(final int x, final int y, final int z, final BasicEntityNode prev, final ShapeCache shapeCache, final CostGetter costGetter) {
                final BasicPathingUniverse nodeType = getLocationType(x, y, z, shapeCache);
                if (nodeType != BasicPathingUniverse.BLOCKED) {
                    final boolean wasInAir = !prev.type.floor;
                    final boolean isInAir = !nodeType.floor;
                    double cost = prev.cost + nodeType.costMultiplier;
                    final double blockHeight = shapeCache.getCollisionShape(x, y, z).getMax(Direction.Axis.Y);
                    EntityState state = prev.state;
                    if (wasInAir & !isInAir & !nodeType.air) {
                        final double fallDistance = prev.fallBlocks + (1 - blockHeight);
                        if (fallDistance >= 4) {
                            final double damage = fallDistance - 4.0;
                            if (damage > state.health) {
                                return null;
                            }
                            cost = cost + AiExMathUtil.adjustableAsymptote(damage, state.health, 16, 4);
                            state = state.withHealth(state.health - damage);
                        }
                    }
                    if (costGetter.cost(BlockPos.asLong(x, y, z)) > cost) {
                        final EntityMovementType type = nodeType == BasicPathingUniverse.LADDER ? EntityMovementType.LADDER_CLIMB : isInAir ? EntityMovementType.FALL : EntityMovementType.WALK;
                        return new BasicEntityNode(x, y, z, prev, prev.cost + (wasInAir & !isInAir && !nodeType.air ? prev.fallBlocks * 0.25 + nodeType.costMultiplier : nodeType.costMultiplier), prev.pathLength + 1, isInAir ? prev.fallBlocks + (1 - blockHeight) : 0, nodeType, getFlags(x, y, z, shapeCache), type, state);
                    }
                }
                return null;
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
                } else if (previous.movementType == EntityMovementType.JUMP) {
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
                }
                if (previous.type.air) {
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
    protected BasicEntityNode createCurrent(final ShapeCache cache, final EntityContext context) {
        final int x = entity.getBlockX();
        final int y = entity.getBlockY();
        final int z = entity.getBlockZ();
        final BasicPathingUniverse type = BasicPathingUniverse.CLASSIFIER.get(x, y, z, cache);
        return new BasicEntityNode(x, y, z, null, 0, 0, 0, type, getFlags(x, y, z, cache), type.floor ? EntityMovementType.WALK : EntityMovementType.JUMP, new EntityState(context.entity().getHealth()));
    }

    @Override
    protected EntityContext createContext(final ShapeCache cache, final double error, final double maxPathLength, final boolean partial, final double urgency) {
        return new EntityContext() {
            @Override
            public LivingEntity entity() {
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
    protected List<NpcMovementNode> convert(final List<BasicEntityNode> nodes) {
        final List<NpcMovementNode> movementNodes = new ArrayList<>(nodes.size());
        final int size = nodes.size();
        for (final int[] i = new int[]{0}; i[0] < size; i[0]++) {
            final NodeInfo node = convert(nodes, i, size);
            NpcMovementNode movementNode = node.node;
            for (int j = node.startIndex; j < node.endIndex; j++) {
                movementNode = wrap(nodes, movementNode, j);
            }
            movementNodes.add(movementNode);
        }
        return movementNodes;
    }

    protected NodeInfo convert(final List<BasicEntityNode> nodes, final int[] index, final int size) {
        int i = index[0];
        final BasicEntityNode node = nodes.get(i);
        final BasicEntityNode next = i + 1 == size ? null : nodes.get(i + 1);
        if (node.type == BasicPathingUniverse.LADDER) {
            return new NodeInfo(new NpcMovementNode.ClimbLadder(node.x + 0.5, node.y + 1, node.z + 0.5), i, i + 1);
        } else if (next != null && next.movementType == EntityMovementType.JUMP) {
            final int start = i;
            while (i < size && !nodes.get(i).type.floor) {
                i = ++index[0];
            }
            final BasicEntityNode endOfJump;
            if (i == size) {
                endOfJump = nodes.get(size - 1);
            } else {
                endOfJump = nodes.get(i);
                index[0]++;
            }
            return new NodeInfo(new NpcMovementNode.Jump(endOfJump.x + 0.5, endOfJump.y, endOfJump.z + 0.5), start, index[0]);
        } else if (node.movementType == EntityMovementType.FALL) {
            return new NodeInfo(new NpcMovementNode.Fall(node.x + 0.5, node.y + 0.5, node.z + 0.5), i, i + 1);
        } else {
            return new NodeInfo(new NpcMovementNode.Walk(node.x + 0.5, node.y, node.z + 0.5), i, i + 1);
        }
    }

    protected NpcMovementNode wrap(final List<BasicEntityNode> nodes, final NpcMovementNode cursor, final int index) {
        final BasicEntityNode node = nodes.get(index);
        if (node.type == BasicPathingUniverse.OPENABLE_DOOR && index + 1 < nodes.size()) {
            final BasicEntityNode next = nodes.get(index + 1);
            final Direction going = Direction.getFacing(next.x - node.x, next.y - node.y, next.z - node.z);
            return new NpcMovementNode.OpenDoor(cursor, going, node.x, node.y, node.z);
        }
        return cursor;
    }

    protected record NodeInfo(NpcMovementNode node, int startIndex, int endIndex) {
    }


    public static final class EntityState {
        private final double health;

        public EntityState(final double health) {
            this.health = health;
        }

        public EntityState withHealth(final double h) {
            if (h != health) {
                return new EntityState(h);
            }
            return this;
        }
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
        private final double fallBlocks;
        private final BasicPathingUniverse type;
        private final long flags;
        private final EntityMovementType movementType;
        private final EntityState state;

        public BasicEntityNode(
                final int x,
                final int y,
                final int z,
                final BasicEntityNode previous,
                final double cost,
                final double pathLength,
                final double fallBlocks,
                final BasicPathingUniverse type,
                final long flags,
                final EntityMovementType movementType,
                final EntityState state
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
            this.state = state;
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
}
