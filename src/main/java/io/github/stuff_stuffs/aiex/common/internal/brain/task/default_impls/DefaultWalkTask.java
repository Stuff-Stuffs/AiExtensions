package io.github.stuff_stuffs.aiex.common.internal.brain.task.default_impls;

import io.github.stuff_stuffs.aiex.common.api.AiExApi;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNodes;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.TaskBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.entity.EntityPather;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiFunction;

public class DefaultWalkTask<T extends Entity> implements BrainNode<T, BasicTasks.Walk.Result, BrainResourceRepository> {
    private final Vec3d target;
    private final double maxError;
    private final double urgency;
    private @Nullable BrainResources.Token token = null;

    public DefaultWalkTask(final Vec3d target, final double maxError, final double urgency) {
        this.target = target;
        this.maxError = maxError;
        this.urgency = urgency;
    }

    @Override
    public void init(final BrainContext<T> context, final SpannedLogger logger) {
    }

    @Override
    public BasicTasks.Walk.Result tick(final BrainContext<T> context, final BrainResourceRepository arg, final SpannedLogger logger) {
        try (final var l = logger.open("DefaultWalkImpl")) {
            if (token == null || !token.active()) {
                final Optional<BrainResources.Token> token = context.brain().resources().get(BrainResource.BODY_CONTROL);
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
                if (!findInitialPath(navigator)) {
                    return BasicTasks.Walk.Result.CANNOT_REACH;
                }
            }
            final EntityPather navigator = AiExApi.ENTITY_NAVIGATOR.find(context.entity(), null);
            if (navigator == null) {
                l.error("Cannot find EntityNavigator!");
                return BasicTasks.Walk.Result.RESOURCE_ACQUISITION_ERROR;
            }
            if (!navigator.idle()) {
                return BasicTasks.Walk.Result.CONTINUE;
            }
            return context.entity().getPos().squaredDistanceTo(target) <= maxError * maxError ? BasicTasks.Walk.Result.DONE : BasicTasks.Walk.Result.CANNOT_REACH;
        }
    }

    private <N extends EntityPather.EntityNode<N>> boolean findInitialPath(final EntityPather pather) {
        return pather.startFollowingPath(new EntityPather.SingleTarget(target), 1.0, 16, true, urgency);
    }

    @Override
    public void deinit(final BrainContext<T> context, final SpannedLogger logger) {
        try (final var l = logger.open("DefaultWalkImpl")) {
            if (token != null && token.active()) {
                l.debug("Releasing boy token");
                context.brain().resources().release(token);
            }
        }
    }

    public static <T extends Entity> BrainNode<T, BasicTasks.Walk.Result, BrainResourceRepository> dynamic(final BasicTasks.Walk.DynamicParameters parameters) {
        final MutableObject<Vec3d> last = new MutableObject<>(parameters.target());
        return BrainNodes.expectResult(new TaskBrainNode<>(BasicTasks.Walk.KEY, (BiFunction<BrainResourceRepository, BrainContext<T>, BasicTasks.Walk.Parameters>) (repository, context) -> parameters, (arg, context) -> arg).resetOnContext((context, repository) -> {
            final double r = parameters.maxError() * 0.25;
            final Vec3d current = parameters.target();
            if (last.getValue().squaredDistanceTo(current) > r * r) {
                last.setValue(current);
                return true;
            }
            return false;
        }), () -> new RuntimeException("No applicable task factory found!"));
    }
}
