package io.github.stuff_stuffs.aiex.common.api.entity.pathing;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.AStar;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.ShapeCache;
import io.github.stuff_stuffs.aiex.common.api.AiExApi;
import io.github.stuff_stuffs.aiex.common.api.debug.AiExDebugFlags;
import io.github.stuff_stuffs.aiex.common.internal.debug.PathDebugInfo;
import io.github.stuff_stuffs.aiex.common.api.entity.AbstractNpcEntity;
import io.github.stuff_stuffs.aiex.common.api.entity.movement.NpcMovementNode;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public abstract class AbstractNpcEntityPather<C extends EntityPather.EntityContext, N> implements EntityPather {
    protected static final double INV_ERROR_SCALE = 0.1;
    protected final AbstractNpcEntity entity;
    private final AStar<N, C, Target> pathfinder;
    private @Nullable LastPathData oldData = null;
    private OutstandingPathRequest<C, N> outstandingRequest;

    public AbstractNpcEntityPather(final AbstractNpcEntity entity) {
        this.entity = entity;
        pathfinder = createPathfinder();
    }

    protected abstract AStar<N, C, Target> createPathfinder();

    protected abstract N createCurrent(final ShapeCache cache, C context);

    protected abstract C createContext(ShapeCache cache, final double error, final double maxPathLength, final boolean partial, final double urgency);

    protected abstract List<NpcMovementNode> convert(List<N> nodes);

    @Override
    public boolean startFollowingPath(final Target target, final double error, final double maxPathLength, final boolean partial, final double urgency, final boolean immediate) {
        final BlockPos pos = entity.getBlockPos();
        final ShapeCache cache = ShapeCache.create(entity.getEntityWorld(), pos.add(-64, -64, -64), pos.add(64, 64, 64), 4096);
        final C context = createContext(cache, error, maxPathLength, partial, urgency);
        final N start = createCurrent(cache, context);
        final LastPathData data = new LastPathData(target, error, maxPathLength, partial, urgency, immediate);
        if (target instanceof SingleTarget single) {
            if (single.target().squaredDistanceTo(entity.getPos()) < error * error) {
                setPath(null, data);
                return true;
            }
        } else if (target instanceof MetricTarget metricTarget) {
            if (metricTarget.score(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ(), context) < error) {
                setPath(null, data);
                return true;
            }
        }
        if (immediate) {
            final AStar.PathInfo<N> path = pathfinder.findPath(start, context, data.target, data.maxError * INV_ERROR_SCALE, data.partial);
            return setPath(path, data);
        } else {
            if (outstandingRequest != null && outstandingRequest.finished.getAcquire()) {
                outstandingRequest.data.setRelease(data);
                if (!outstandingRequest.finished.getAcquire()) {
                    return true;
                }
            }
            outstandingRequest = new OutstandingPathRequest<>(data, pathfinder, context, start, cache, this::setPath);
            AiExApi.submitTask(outstandingRequest, (ServerWorld) entity.getEntityWorld());
            return true;
        }
    }

    protected boolean setPath(final AStar.PathInfo<N> path, final LastPathData oldData) {
        if (path == null || path.path() == null) {
            this.oldData = null;
            entity.getNpcMoveControl().set(List.of());
            return false;
        }
        this.oldData = oldData;
        entity.getNpcMoveControl().set(convert(path.path()));
        AiExDebugFlags.send(AiExDebugFlags.PATH_FLAG, debugInfo(path), (ServerWorld) entity.getEntityWorld());
        return true;
    }

    protected abstract PathDebugInfo debugInfo(AStar.PathInfo<N> path);

    @Override
    public boolean idle() {
        return entity.getNpcMoveControl().idle();
    }

    @Override
    public void tick() {
        if (entity.getNpcMoveControl().failedLastPath() && oldData != null) {
            startFollowingPath(oldData.target, oldData.maxError, oldData.maxPathLength, oldData.partial, oldData.urgency, oldData.immediate);
        }
    }

    protected record LastPathData(Target target, double maxError, double maxPathLength, boolean partial, double urgency,
                                  boolean immediate) {
    }

    protected static class OutstandingPathRequest<C extends EntityPather.EntityContext, N> implements AiExApi.Job {
        protected final AtomicReference<LastPathData> data;
        protected final AStar<N, C, Target> pathfinder;
        protected final C context;
        protected final N start;
        protected final ShapeCache cache;
        protected final BiConsumer<AStar.PathInfo<N>, LastPathData> consumer;
        protected final AtomicBoolean finished;

        public OutstandingPathRequest(final LastPathData data, final AStar<N, C, Target> pathfinder, final C context, final N start, final ShapeCache cache, final BiConsumer<AStar.PathInfo<N>, LastPathData> consumer) {
            this.data = new AtomicReference<>(data);
            this.pathfinder = pathfinder;
            this.context = context;
            this.start = start;
            this.cache = cache;
            this.consumer = consumer;
            finished = new AtomicBoolean(false);
        }

        @Override
        public void run() {
            if (finished.getAcquire()) {
                return;
            }
            finished.setRelease(true);
            final LastPathData data = this.data.getAcquire();
            final AStar.PathInfo<N> path = pathfinder.findPath(start, context, data.target, data.maxError * INV_ERROR_SCALE, data.partial);
            consumer.accept(path, data);
        }

        @Override
        public void preRun() {
        }
    }
}
