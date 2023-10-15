package io.github.stuff_stuffs.aiex.common.internal.brain.task.default_impls;

import io.github.stuff_stuffs.aiex.common.api.AiExApi;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNodes;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.TaskTerminalBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.entity.EntityNavigator;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiFunction;

public class DefaultWalkTask<T extends Entity> implements BrainNode<T, BasicTasks.Walk.Result, BrainResourceRepository> {
    private final Vec3d target;
    private final double maxError;
    private @Nullable BrainResources.Token token = null;

    public DefaultWalkTask(final Vec3d target, final double maxError) {
        this.target = target;
        this.maxError = maxError;
    }

    @Override
    public void init(final BrainContext<T> context) {
    }

    @Override
    public BasicTasks.Walk.Result tick(final BrainContext<T> context, final BrainResourceRepository arg) {
        if (token == null || !token.active()) {
            final Optional<BrainResources.Token> token = context.brain().resources().get(BrainResource.BODY_CONTROL);
            if (token.isEmpty()) {
                return BasicTasks.Walk.Result.RESOURCE_ACQUISITION_ERROR;
            }
            this.token = token.get();
        }
        final EntityNavigator navigator = AiExApi.ENTITY_NAVIGATOR.find(context.entity(), null);
        if (navigator == null) {
            throw new IllegalStateException();
        }
        final boolean done = navigator.walkTo(target, maxError);
        if (!done) {
            return BasicTasks.Walk.Result.CONTINUE;
        }
        return context.entity().getPos().squaredDistanceTo(target) <= maxError * maxError ? BasicTasks.Walk.Result.DONE : BasicTasks.Walk.Result.CANNOT_REACH;
    }

    @Override
    public void deinit(final BrainContext<T> context) {
        if (token != null && token.active()) {
            context.brain().resources().release(token);
        }
    }

    public static <T extends Entity> BrainNode<T, BasicTasks.Walk.Result, BrainResourceRepository> dynamic(final BasicTasks.Walk.DynamicParameters parameters) {
        final MutableObject<Vec3d> last = new MutableObject<>(parameters.target());
        return BrainNodes.expectResult(new TaskTerminalBrainNode<>(BasicTasks.Walk.KEY, (BiFunction<BrainResourceRepository, BrainContext<T>, BasicTasks.Walk.Parameters>) (repository, context) -> parameters).resetOnContext((context, repository) -> {
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
