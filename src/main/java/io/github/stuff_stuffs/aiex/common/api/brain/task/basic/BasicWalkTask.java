package io.github.stuff_stuffs.aiex.common.api.brain.task.basic;

import io.github.stuff_stuffs.aiex.common.api.AiExApi;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import io.github.stuff_stuffs.aiex.common.api.brain.task.*;
import io.github.stuff_stuffs.aiex.common.api.entity.EntityNavigator;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BasicWalkTask implements Task<BasicTasks.Walk.Result, Entity> {
    private final Vec3d target;
    private final double maxError;
    private @Nullable BrainResources.Token token = null;

    public BasicWalkTask(final Vec3d target, final double maxError) {
        this.target = target;
        this.maxError = maxError;
    }

    @Override
    public BasicTasks.Walk.Result run(final BrainContext<Entity> context) {
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
    public void stop(final BrainContext<Entity> context) {
        if (token != null && token.active()) {
            context.brain().resources().release(token);
        }
    }

    public static Task<BasicTasks.Walk.Result, Entity> dynamic(final BasicTasks.Walk.DynamicParameters parameters) {
        return Tasks.expect(new SelectorPairTask<>(context -> {
            final MutableObject<Vec3d> last = new MutableObject<>(parameters.target());
            return Tasks.expect(new ContextResetTask<>(ctx -> ctx.createTask(BasicTasks.Walk.KEY, parameters).orElse(null), ctx -> {
                final double r = parameters.maxError() * 0.25;
                final Vec3d current = parameters.target();
                if (last.getValue().squaredDistanceTo(current) > r * r) {
                    last.setValue(current);
                    return true;
                }
                return false;
            }), () -> new RuntimeException("Walk task factory error!"));
        }, context -> Tasks.constant(BasicTasks.Walk.Result.DONE), ctx -> !parameters.shouldStop(), true), () -> new RuntimeException("wtf!"));
    }
}
