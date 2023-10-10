package io.github.stuff_stuffs.aiex.common.api;

import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import io.github.stuff_stuffs.aiex.common.api.brain.task.*;
import io.github.stuff_stuffs.aiex.common.api.entity.EntityNavigator;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class AiExApi {
    public static final EntityApiLookup<EntityNavigator, Void> ENTITY_NAVIGATOR = EntityApiLookup.get(AiExCommon.id("basic_navigator"), EntityNavigator.class, Void.class);

    public static void init() {
        ENTITY_NAVIGATOR.registerFallback((entity, context) -> {
            if (entity instanceof MobEntity mob) {
                return (pos, error) -> {
                    final EntityNavigation navigation = mob.getNavigation();
                    final Path path = navigation.findPathTo(pos.x, pos.y, pos.z, (int) Math.floor(error));
                    if (path == null) {
                        return true;
                    }
                    navigation.startMovingAlong(path, 0.5);
                    return navigation.isIdle();
                };
            } else {
                return null;
            }
        });
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
                        TaskConfig.Factory<T, BasicTasks.Walk.Result, BasicTasks.Walk.DynamicParameters> basic = (TaskConfig.Factory<T, BasicTasks.Walk.Result, BasicTasks.Walk.DynamicParameters>) (TaskConfig.Factory<Entity, BasicTasks.Walk.Result, BasicTasks.Walk.DynamicParameters>) AiExApi::dynamicWalk;
                        if (builder.hasFactory(BasicTasks.Walk.DYNAMIC_KEY)) {
                            final TaskConfig.Factory<T, BasicTasks.Walk.Result, BasicTasks.Walk.DynamicParameters> current = builder.getFactory(BasicTasks.Walk.DYNAMIC_KEY);
                            basic = current.fallbackTo(basic);
                        }
                        builder.putFactory(BasicTasks.Walk.DYNAMIC_KEY, basic);
                    }
                }
                if (entity instanceof LivingEntity) {
                    {
                        //noinspection unchecked
                        TaskConfig.Factory<T, BasicTasks.Look.Result, BasicTasks.Look.Parameters> basic = (TaskConfig.Factory<T, BasicTasks.Look.Result, BasicTasks.Look.Parameters>) (TaskConfig.Factory<LivingEntity, BasicTasks.Look.Result, BasicTasks.Look.Parameters>) parameters -> new SimpleLookTask(parameters.lookDir(), parameters.lookSpeed());
                        if (builder.hasFactory(BasicTasks.Look.KEY)) {
                            final TaskConfig.Factory<T, BasicTasks.Look.Result, BasicTasks.Look.Parameters> current = builder.getFactory(BasicTasks.Look.KEY);
                            basic = current.fallbackTo(basic);
                        }
                        builder.putFactory(BasicTasks.Look.KEY, basic);
                    }
                    {
                        //noinspection unchecked
                        TaskConfig.Factory<T, BasicTasks.Look.Result, BasicTasks.Look.Parameters> basic = (TaskConfig.Factory<T, BasicTasks.Look.Result, BasicTasks.Look.Parameters>) (TaskConfig.Factory<LivingEntity, BasicTasks.Look.Result, BasicTasks.Look.Parameters>) AiExApi::dynamicLook;
                        if (builder.hasFactory(BasicTasks.Look.DYNAMIC_KEY)) {
                            final TaskConfig.Factory<T, BasicTasks.Look.Result, BasicTasks.Look.Parameters> current = builder.getFactory(BasicTasks.Look.DYNAMIC_KEY);
                            basic = current.fallbackTo(basic);
                        }
                        builder.putFactory(BasicTasks.Look.DYNAMIC_KEY, basic);
                    }
                }
            }
        });
    }

    private static final class SimpleLookTask implements Task<BasicTasks.Look.Result, LivingEntity> {
        private final Vec3d lookDir;
        private final double lookSpeed;
        private @Nullable BrainResources.Token headToken = null;
        private @Nullable BrainResources.Token bodyToken = null;

        private SimpleLookTask(final Vec3d dir, final double speed) {
            lookDir = dir.normalize();
            lookSpeed = speed;
        }

        @Override
        public BasicTasks.Look.Result run(final BrainContext<LivingEntity> context) {
            if (headToken == null || !headToken.active()) {
                headToken = context.brain().resources().get(BrainResource.HEAD_CONTROL).orElse(null);
                if (headToken == null) {
                    return BasicTasks.Look.Result.RESOURCE_ACQUISITION_ERROR;
                }
            }
            final LivingEntity entity = context.entity();
            final Vec3d vector = entity.getRotationVec(1.0F);
            final Vec3d endLook = constantSpeedSlerp(vector, lookDir, lookSpeed);
            final float headYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(endLook.z, endLook.x)) - 90.0F);
            final double len = Math.sqrt(endLook.x * endLook.x + endLook.z * endLook.z);
            final float pitch = (float) MathHelper.wrapDegrees(Math.toDegrees(-Math.atan2(endLook.y, len)));
            if (MathHelper.angleBetween(headYaw, entity.getBodyYaw()) > 60.0F) {
                if (bodyToken == null || !bodyToken.active()) {
                    bodyToken = context.brain().resources().get(BrainResource.HEAD_CONTROL).orElse(null);
                    if (bodyToken == null) {
                        return BasicTasks.Look.Result.FAILED;
                    }
                }
                entity.setHeadYaw(headYaw);
                entity.setPitch(pitch);
                final float bodyYaw;
                final float lower = MathHelper.angleBetween(entity.getBodyYaw(), MathHelper.wrapDegrees(headYaw - 60.0F));
                final float upper = MathHelper.angleBetween(entity.getBodyYaw(), MathHelper.wrapDegrees(headYaw + 60.0F));
                if (lower < upper) {
                    bodyYaw = MathHelper.wrapDegrees(headYaw - 60.0F);
                } else {
                    bodyYaw = MathHelper.wrapDegrees(headYaw + 60.0F);
                }
                entity.setBodyYaw(bodyYaw);
            } else {
                if (bodyToken != null && bodyToken.active()) {
                    context.brain().resources().release(bodyToken);
                    bodyToken = null;
                }
                entity.setHeadYaw(headYaw);
                entity.setPitch(pitch);
                if (MathHelper.angleBetween(headYaw, entity.getBodyYaw()) > 1.5F) {
                    if (bodyToken == null || !bodyToken.active()) {
                        bodyToken = context.brain().resources().get(BrainResource.HEAD_CONTROL).orElse(null);
                    }
                    if (bodyToken != null && bodyToken.active()) {
                        final float bodyYaw;
                        final float lower = MathHelper.angleBetween(entity.getBodyYaw(), MathHelper.wrapDegrees(headYaw - 1.5F));
                        final float upper = MathHelper.angleBetween(entity.getBodyYaw(), MathHelper.wrapDegrees(headYaw + 1.5F));
                        if (lower < upper) {
                            bodyYaw = MathHelper.wrapDegrees(headYaw - 1.5F);
                        } else {
                            bodyYaw = MathHelper.wrapDegrees(headYaw + 1.5F);
                        }
                        entity.setYaw(bodyYaw);
                    }
                } else {
                    if (bodyToken != null && bodyToken.active()) {
                        context.brain().resources().release(bodyToken);
                    }
                }
            }
            return vector.dotProduct(endLook) > 0.99 ? BasicTasks.Look.Result.ALIGNED : BasicTasks.Look.Result.CONTINUE;
        }

        @Override
        public void stop(final AiBrainView context) {
            if (headToken != null && headToken.active()) {
                context.resources().release(headToken);
            }
            if (bodyToken != null && bodyToken.active()) {
                context.resources().release(bodyToken);
            }
        }

        private static Vec3d constantSpeedSlerp(final Vec3d start, final Vec3d end, final double stepSize) {
            final double omega = Math.acos(start.dotProduct(end));
            if (Math.abs(omega) < 0.00001) {
                return end;
            }
            final double t = 0.5;//Math.min(stepSize * omega, 1.0);
            if (t > 0.9999) {
                return end;
            } else if (t < 0.0001) {
                return start;
            }
            final double so = Math.sin(omega);
            final double s1to = Math.sin((1 - t) * omega);
            final double sto = Math.sin(t * omega);
            return start.multiply(s1to / so).add(end.multiply(sto / so));
        }
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

    private static Task<BasicTasks.Walk.Result, Entity> dynamicWalk(final BasicTasks.Walk.DynamicParameters parameters) {
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

    private static Task<BasicTasks.Look.Result, LivingEntity> dynamicLook(final BasicTasks.Look.Parameters parameters) {
        final MutableObject<Vec3d> last = new MutableObject<>(parameters.lookDir().normalize());
        final MutableDouble lastSpeed = new MutableDouble(parameters.lookSpeed());
        return Tasks.expect(new ContextResetTask<>(ctx -> ctx.createTask(BasicTasks.Look.KEY, parameters).orElse(null), ctx -> {
            final Vec3d dir = parameters.lookDir().normalize();
            final double speed = parameters.lookSpeed();
            if (dir.dotProduct(last.getValue()) < 1 || Math.abs(speed - lastSpeed.doubleValue()) > 0.05) {
                last.setValue(dir);
                lastSpeed.setValue(speed);
                return true;
            }
            return false;
        }), () -> new RuntimeException("Look task factory error!"));
    }

    private AiExApi() {
    }
}
