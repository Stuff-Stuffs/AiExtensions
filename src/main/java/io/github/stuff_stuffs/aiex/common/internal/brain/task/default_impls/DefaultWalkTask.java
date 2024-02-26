package io.github.stuff_stuffs.aiex.common.internal.brain.task.default_impls;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.ShapeCache;
import io.github.stuff_stuffs.aiex.common.api.AiExApi;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.EntityPather;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DefaultWalkTask<T extends LivingEntity> implements BrainNode<T, BasicTasks.Walk.Result, BrainResourceRepository> {
    private final EntityPather.Target target;
    private final double maxError;
    private final double urgency;
    private final double maxPathLength;
    private final boolean partial;
    private @Nullable BrainResources.Token token = null;
    private boolean started = false;

    public DefaultWalkTask(final EntityPather.Target target, final double maxError, final double urgency, final double maxPathLength, final boolean partial) {
        this.target = target;
        this.maxError = maxError;
        this.urgency = urgency;
        this.maxPathLength = maxPathLength;
        this.partial = partial;
    }

    @Override
    public void init(final BrainContext<T> context, final SpannedLogger logger) {
        started = false;
    }

    @Override
    public BasicTasks.Walk.Result tick(final BrainContext<T> context, final BrainResourceRepository arg, final SpannedLogger logger) {
        try (final var l = logger.open("DefaultWalkImpl")) {
            if (token == null || !token.active()) {
                if (started) {
                    final EntityPather navigator = AiExApi.ENTITY_NAVIGATOR.find(context.entity(), null);
                    if (navigator != null) {
                        navigator.stop();
                    }
                    started = false;
                }
                final Optional<BrainResources.Token> token = context.brain().resources().get(BrainResource.ACTIVE_BODY_CONTROL);
                l.debug("Trying to acquire body token");
                if (token.isEmpty()) {
                    l.debug("Failed");
                    return BasicTasks.Walk.Result.RESOURCE_ACQUISITION_ERROR;
                }
                l.debug("Success");
                this.token = token.get();
                final EntityPather navigator = AiExApi.ENTITY_NAVIGATOR.find(context.entity(), null);
                if (navigator == null) {
                    l.error("Cannot find EntityNavigator!");
                    return BasicTasks.Walk.Result.RESOURCE_ACQUISITION_ERROR;
                }
                if (!navigator.startFollowingPath(target, maxError, maxPathLength, partial, urgency)) {
                    return BasicTasks.Walk.Result.CANNOT_REACH;
                }
                started = true;
            }
            final EntityPather navigator = AiExApi.ENTITY_NAVIGATOR.find(context.entity(), null);
            if (navigator == null) {
                l.error("Cannot find EntityNavigator!");
                return BasicTasks.Walk.Result.RESOURCE_ACQUISITION_ERROR;
            }
            if (!navigator.idle()) {
                return BasicTasks.Walk.Result.CONTINUE;
            }
            final boolean closeEnough;
            if (target instanceof EntityPather.SingleTarget singleTarget) {
                closeEnough = context.entity().getPos().squaredDistanceTo(singleTarget.target()) <= maxError * maxError;
            } else if (target instanceof EntityPather.MetricTarget metricTarget) {
                final BlockPos pos = context.entity().getBlockPos();
                final ShapeCache shapeCache = ShapeCache.createUnbounded(context.world(), 1024);
                closeEnough = metricTarget.score(pos.getX(), pos.getY(), pos.getZ(), new EntityPather.EntityContext() {
                    @Override
                    public LivingEntity entity() {
                        return context.entity();
                    }

                    @Override
                    public double maxPathLength() {
                        return maxPathLength;
                    }

                    @Override
                    public ShapeCache cache() {
                        return shapeCache;
                    }
                }) < maxError;
            } else {
                throw new AssertionError();
            }
            return closeEnough ? BasicTasks.Walk.Result.DONE : BasicTasks.Walk.Result.CANNOT_REACH;
        }
    }

    @Override
    public void deinit(final BrainContext<T> context, final SpannedLogger logger) {
        try (final var l = logger.open("DefaultWalkImpl")) {
            if (token != null && token.active()) {
                l.debug("Releasing body token");
                context.brain().resources().release(token);
            }
        }
    }
}
