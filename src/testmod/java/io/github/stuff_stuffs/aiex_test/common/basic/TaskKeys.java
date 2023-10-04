package io.github.stuff_stuffs.aiex_test.common.basic;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.task.Task;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskKey;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.Vec3d;

public final class TaskKeys {
    public static final TaskKey<WalkTask.Result, WalkTask.Parameters> WALK_TASK_KEY = new TaskKey<>(WalkTask.Result.class, WalkTask.Parameters.class);

    public static void init() {
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("walk_task"), WALK_TASK_KEY);
        TaskConfig.ON_BUILD_EVENT.register(TaskConfig.OnBuild.DEFAULTS_PHASE, new TaskConfig.OnBuild() {
            @Override
            public <T> void onBuild(final T entity, final TaskConfig.Builder<T> builder) {
                if (entity instanceof Entity e) {
                    final WalkTask.Navigator navigator = AiExApi.ENTITY_NAVIGATOR.find(e, null);
                    if (navigator != null) {
                        //noinspection unchecked
                        TaskConfig.Factory<T, WalkTask.Result, WalkTask.Parameters> basic = (TaskConfig.Factory<T, WalkTask.Result, WalkTask.Parameters>) (TaskConfig.Factory<Entity, WalkTask.Result, WalkTask.Parameters>) parameters -> new SimpleWalkTask(parameters.target(), parameters.maxError());
                        if (builder.hasFactory(WALK_TASK_KEY)) {
                            final TaskConfig.Factory<T, WalkTask.Result, WalkTask.Parameters> current = builder.getFactory(WALK_TASK_KEY);
                            basic = current.fallbackTo(basic);
                        }
                        builder.putFactory(WALK_TASK_KEY, basic);
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
        public WalkTask.Result run(final BrainContext<? extends Entity> context) {
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

    private TaskKeys() {
    }
}
