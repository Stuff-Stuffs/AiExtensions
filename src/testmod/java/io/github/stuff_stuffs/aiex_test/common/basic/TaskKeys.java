package io.github.stuff_stuffs.aiex_test.common.basic;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.task.Task;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskKey;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.FuzzyPositions;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;

public final class TaskKeys {
    public static final TaskKey<WalkTask.Result, WalkTask.Parameters> WALK_TASK_KEY = new TaskKey<>(WalkTask.Result.class, WalkTask.Parameters.class);
    public static final TaskKey<WanderTask.Result, WanderTask.Parameters> WANDER_TASK_KEY = new TaskKey<>(WanderTask.Result.class, WanderTask.Parameters.class);

    public static void init() {
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("walk"), WALK_TASK_KEY);
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("wander"), WANDER_TASK_KEY);
        TaskConfig.ON_BUILD_EVENT.register(TaskConfig.OnBuild.DEFAULTS_PHASE, new TaskConfig.OnBuild() {
            @Override
            public <T> void onBuild(final T entity, final TaskConfig.Builder<T> builder) {
                if (entity instanceof Entity e) {
                    final WalkTask.Navigator navigator = AiExApi.ENTITY_NAVIGATOR.find(e, null);
                    if (navigator != null) {
                        {
                            //noinspection unchecked
                            TaskConfig.Factory<T, WalkTask.Result, WalkTask.Parameters> basic = (TaskConfig.Factory<T, WalkTask.Result, WalkTask.Parameters>) (TaskConfig.Factory<Entity, WalkTask.Result, WalkTask.Parameters>) parameters -> new SimpleWalkTask(parameters.target(), parameters.maxError());
                            if (builder.hasFactory(WALK_TASK_KEY)) {
                                final TaskConfig.Factory<T, WalkTask.Result, WalkTask.Parameters> current = builder.getFactory(WALK_TASK_KEY);
                                basic = current.fallbackTo(basic);
                            }
                            builder.putFactory(WALK_TASK_KEY, basic);
                        }
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

    private static final class SimpleWalkTask implements Task<WalkTask.Result, Entity> {
        private final Vec3d target;
        private final double maxError;

        private SimpleWalkTask(final Vec3d target, final double maxError) {
            this.target = target;
            this.maxError = maxError;
        }

        @Override
        public WalkTask.Result run(final BrainContext<Entity> context) {
            final WalkTask.Navigator navigator = AiExApi.ENTITY_NAVIGATOR.find(context.entity(), null);
            if (navigator == null) {
                throw new IllegalStateException();
            }
            final boolean done = navigator.walkTo(target, maxError);
            if (!done) {
                return WalkTask.Result.CONTINUE;
            }
            return context.entity().getPos().squaredDistanceTo(target) <= maxError * maxError ? WalkTask.Result.DONE : WalkTask.Result.FAILED;
        }
    }

    private static final class SimpleWanderTask implements Task<WanderTask.Result, Entity> {
        private final Vec3d center;
        private final double range;
        private Task<WalkTask.Result, Entity> currentWalk;

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
                currentWalk = context.createTask(WALK_TASK_KEY, new WalkTask.Parameters() {
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
            final WalkTask.Result result = currentWalk.run(context);
            if (result == WalkTask.Result.FAILED) {
                return WanderTask.Result.FAILED;
            }
            if (result == WalkTask.Result.DONE) {
                currentWalk = null;
            }
            return WanderTask.Result.SUCCESS;
        }
    }

    private TaskKeys() {
    }
}
