package io.github.stuff_stuffs.aiex_test.common.basic;

import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import io.github.stuff_stuffs.aiex.common.api.brain.task.*;
import io.github.stuff_stuffs.aiex.common.api.entity.EntityNavigator;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.FuzzyPositions;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class TaskKeys {
    public static final TaskKey<WanderTask.Result, WanderTask.Parameters> WANDER_TASK_KEY = new TaskKey<>(WanderTask.Result.class, WanderTask.Parameters.class);

    public static void init() {
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("wander"), WANDER_TASK_KEY);
        TaskConfig.ON_BUILD_EVENT.register(TaskConfig.OnBuild.DEFAULTS_PHASE, new TaskConfig.OnBuild() {
            @Override
            public <T> void onBuild(final T entity, final TaskConfig.Builder<T> builder) {
                if (entity instanceof Entity e) {
                    final EntityNavigator navigator = AiExApi.ENTITY_NAVIGATOR.find(e, null);
                    if (navigator != null) {
                        //noinspection unchecked
                        TaskConfig.Factory<T, BasicTasks.Walk.Result, BasicTasks.Walk.Parameters> basic = (TaskConfig.Factory<T, BasicTasks.Walk.Result, BasicTasks.Walk.Parameters>) (TaskConfig.Factory<Entity, BasicTasks.Walk.Result, BasicTasks.Walk.Parameters>) parameters -> new SimpleWalkTask(parameters.target(), parameters.maxError());
                        if (builder.hasFactory(BasicTasks.Walk.KEY)) {
                            final TaskConfig.Factory<T, BasicTasks.Walk.Result, BasicTasks.Walk.Parameters> current = builder.getFactory(BasicTasks.Walk.KEY);
                            basic = current.fallbackTo(basic);
                        }
                        builder.putFactory(BasicTasks.Walk.KEY, basic);
                    }
                    if (builder.hasFactory(BasicTasks.Walk.KEY)) {
                        //noinspection unchecked
                        TaskConfig.Factory<T, BasicTasks.Walk.Result, BasicTasks.Walk.DynamicParameters> basic = (TaskConfig.Factory<T, BasicTasks.Walk.Result, BasicTasks.Walk.DynamicParameters>) (TaskConfig.Factory<Entity, BasicTasks.Walk.Result, BasicTasks.Walk.DynamicParameters>) TaskKeys::dynamicWalk;
                        if (builder.hasFactory(BasicTasks.Walk.DYNAMIC_KEY)) {
                            final TaskConfig.Factory<T, BasicTasks.Walk.Result, BasicTasks.Walk.DynamicParameters> current = builder.getFactory(BasicTasks.Walk.DYNAMIC_KEY);
                            basic = current.fallbackTo(basic);
                        }
                        builder.putFactory(BasicTasks.Walk.DYNAMIC_KEY, basic);
                    }
                    if (builder.hasFactory(WANDER_TASK_KEY)) {
                        //noinspection unchecked
                        TaskConfig.Factory<T, WanderTask.Result, WanderTask.Parameters> basic = (TaskConfig.Factory<T, WanderTask.Result, WanderTask.Parameters>) (TaskConfig.Factory<Entity, WanderTask.Result, WanderTask.Parameters>) parameters -> new SimpleWanderTask(parameters.center(), parameters.range());
                        if (builder.hasFactory(WANDER_TASK_KEY)) {
                            final TaskConfig.Factory<T, WanderTask.Result, WanderTask.Parameters> current = builder.getFactory(WANDER_TASK_KEY);
                            basic = current.fallbackTo(basic);
                        }
                        builder.putFactory(WANDER_TASK_KEY, basic);
                    }
                }
            }
        });
    }


    private static final class SimpleWalkTask implements Task<BasicTasks.Walk.Result, Entity> {
        private final Vec3d target;
        private final double maxError;
        private @Nullable BrainResources.Token token = null;

        private SimpleWalkTask(final Vec3d target, final double maxError) {
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
        public void stop(final AiBrainView context) {
            if (token != null && token.active()) {
                context.resources().release(token);
            }
        }
    }

    public static Task<BasicTasks.Walk.Result, Entity> dynamicWalk(final BasicTasks.Walk.DynamicParameters parameters) {
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
            }), () -> new RuntimeException("Walk task factory missing!"));
        }, context -> Tasks.constant(BasicTasks.Walk.Result.DONE), ctx -> !parameters.shouldStop(), true), () -> new RuntimeException("wtf!"));
    }


    private static final class SimpleWanderTask implements Task<WanderTask.Result, Entity> {
        private final Vec3d center;
        private final double range;
        private Task<BasicTasks.Walk.Result, Entity> currentWalk;

        private SimpleWanderTask(final Vec3d center, final double range) {
            this.center = center;
            this.range = range;
        }

        @Override
        public WanderTask.Result run(final BrainContext<Entity> context) {
            if (currentWalk == null) {
                final Random random = new Xoroshiro128PlusPlusRandom(context.brain().randomSeed());
                final Box originBox = context.entity().getBoundingBox().offset(context.entity().getPos().multiply(-1));
                final Vec3d best = FuzzyPositions.guessBest(() -> {
                    final double x = center.x + random.nextDouble() * range * 2 - range;
                    final double y = center.y + random.nextDouble() * range * 2 - range;
                    final double z = center.z + random.nextDouble() * range * 2 - range;
                    if (context.world().getBlockCollisions(context.entity(), originBox.offset(x, y, z)).iterator().hasNext()) {
                        return null;
                    }
                    return BlockPos.ofFloored(x, y, z);
                }, pos -> 1.0);
                if (best == null) {
                    return WanderTask.Result.FAILED;
                }
                currentWalk = context.createTask(BasicTasks.Walk.KEY, new BasicTasks.Walk.Parameters() {
                    @Override
                    public Vec3d target() {
                        return best;
                    }

                    @Override
                    public double maxError() {
                        return range * 0.1;
                    }
                }).orElse(null);
            }
            if (currentWalk == null) {
                return WanderTask.Result.FAILED;
            }
            final BasicTasks.Walk.Result result = currentWalk.run(context);
            if (result == BasicTasks.Walk.Result.CANNOT_REACH || result == BasicTasks.Walk.Result.RESOURCE_ACQUISITION_ERROR) {
                return WanderTask.Result.FAILED;
            }
            if (result == BasicTasks.Walk.Result.DONE) {
                currentWalk = null;
            }
            return WanderTask.Result.SUCCESS;
        }

        @Override
        public void stop(final AiBrainView context) {

        }
    }

    private TaskKeys() {
    }
}
