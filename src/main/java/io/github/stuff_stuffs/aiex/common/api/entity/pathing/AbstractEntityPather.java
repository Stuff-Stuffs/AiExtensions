package io.github.stuff_stuffs.aiex.common.api.entity.pathing;

import io.github.stuff_stuffs.advanced_ai.common.api.util.AStar;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import net.minecraft.block.BlockState;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractEntityPather<C extends EntityPather.EntityContext, N> implements EntityPather {
    protected static final double INV_ERROR_SCALE = 0.1;
    protected final MobEntity entity;
    private final AStar<N, C, Target> pathfinder;
    private @Nullable EntityPather.Target target = null;
    private double lastMaxError = 0.0;
    private double lastMaxPathLength = 0.0;
    private boolean lastPartial;
    private @Nullable List<Wrapper<N>> currentPath = null;
    private int currentIndex = 0;
    private double urgency = 0.0;

    public AbstractEntityPather(final MobEntity entity) {
        this.entity = entity;
        pathfinder = createPathfinder();
    }

    protected abstract AStar<N, C, Target> createPathfinder();

    protected abstract N createCurrent(final ShapeCache cache);

    protected abstract C createContext(ShapeCache cache, final double error, final double maxPathLength, final boolean partial, final double urgency);

    @Override
    public boolean startFollowingPath(final Target target, final double error, final double maxPathLength, final boolean partial, final double urgency) {
        final BlockPos pos = entity.getBlockPos();
        final ShapeCache cache = ShapeCache.create(entity.getEntityWorld(), pos.add(-64, -64, -64), pos.add(64, 64, 64), 4096);
        final AStar.PathInfo<N> path = pathfinder.findPath(createCurrent(cache), createContext(cache, error, maxPathLength, partial, urgency), target, error * INV_ERROR_SCALE, partial);
        if (path == null || path.path() == null) {
            currentPath = null;
            this.urgency = 0;
            this.target = null;
            return false;
        }
        this.target = target;
        currentPath = new ArrayList<>(path.path().size());
        for (final N node : path.path()) {
            currentPath.add(new Wrapper<>(node));
        }
        currentIndex = 0;
        lastMaxPathLength = maxPathLength;
        lastMaxError = error;
        lastPartial = partial;
        currentPath.get(0).timeoutTimer = 20;
        this.urgency = urgency;
        return true;
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
                startFollowingPath(target, lastMaxError, lastMaxPathLength, lastPartial, urgency);
            }
        }
    }

    protected abstract BlockPos fromNode(N node);

    private void continueAlongPath() {
        assert currentPath != null;
        final double nodeReachProximity = (entity.getWidth() > 0.75F ? entity.getWidth() / 2.0F : 0.75F - entity.getWidth() / 2.0F);
        final BlockPos vec3i = fromNode(currentPath.get(currentIndex).node);
        final double d = Math.abs(entity.getX() - (vec3i.getX() + 0.5));
        final double e = Math.abs(entity.getY() - vec3i.getY());
        final double f = Math.abs(entity.getZ() - (vec3i.getZ() + 0.5));
        final boolean reached = d < nodeReachProximity && f < nodeReachProximity && e < 1.0;
        if (reached) {
            currentIndex++;
            if (currentPath.size() == currentIndex) {
                currentPath = null;
                target = null;
                return;
            }
            currentPath.get(currentIndex).timeoutTimer = 20;
        }
        final BlockPos wrapper = fromNode(currentPath.get(currentIndex).node);
        final double centerizer = (entity.getWidth() + 1 - MathHelper.fractionalPart(entity.getWidth())) * 0.5;
        final double x = wrapper.getX() + centerizer;
        final double y = adjustTargetY(wrapper);
        final double z = wrapper.getZ() + centerizer;
        entity.getMoveControl().moveTo(x, y, z, Math.min(entity.getPos().distanceTo(new Vec3d(x, y, z)) + 0.05, 0.4));
    }

    private double adjustTargetY(final BlockPos pos) {
        final World world = entity.getEntityWorld();
        final BlockState state = world.getBlockState(pos);
        return state.isAir() ? pos.getY() : pos.getY() + state.getCollisionShape(world, pos).getMax(Direction.Axis.Y);
    }

    private static final class Wrapper<T> {
        private final T node;
        private int timeoutTimer = 0;

        private Wrapper(final T node) {
            this.node = node;
        }
    }
}
