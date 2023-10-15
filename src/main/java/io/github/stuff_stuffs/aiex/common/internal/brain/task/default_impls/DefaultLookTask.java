package io.github.stuff_stuffs.aiex.common.internal.brain.task.default_impls;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNodes;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.TaskTerminalBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public class DefaultLookTask<T extends LivingEntity> implements BrainNode<T, BasicTasks.Look.Result, BrainResourceRepository> {
    private final Vec3d lookDir;
    private final double lookSpeed;
    private @Nullable BrainResources.Token headToken = null;
    private @Nullable BrainResources.Token bodyToken = null;

    public DefaultLookTask(final Vec3d dir, final double speed) {
        lookDir = dir.normalize();
        lookSpeed = speed;
    }

    @Override
    public void init(final BrainContext<T> context) {

    }

    @Override
    public BasicTasks.Look.Result tick(final BrainContext<T> context, final BrainResourceRepository arg) {
        if (headToken == null || !headToken.active()) {
            headToken = arg.get(BrainResource.HEAD_CONTROL).orElse(null);
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
        if (MathHelper.angleBetween(headYaw, entity.getYaw()) > 60.0F) {
            if (bodyToken == null || !bodyToken.active()) {
                bodyToken = arg.get(BrainResource.BODY_CONTROL).orElse(null);
                if (bodyToken == null) {
                    return BasicTasks.Look.Result.FAILED;
                }
            }
            entity.setHeadYaw(headYaw);
            entity.setPitch(pitch);
            final float bodyYaw;
            final float lower = MathHelper.angleBetween(entity.getYaw(), MathHelper.wrapDegrees(headYaw - 60.0F));
            final float upper = MathHelper.angleBetween(entity.getYaw(), MathHelper.wrapDegrees(headYaw + 60.0F));
            if (lower < upper) {
                bodyYaw = MathHelper.wrapDegrees(headYaw - 59.0F);
            } else {
                bodyYaw = MathHelper.wrapDegrees(headYaw + 59.0F);
            }
            entity.setYaw(bodyYaw);
        } else {
            entity.setHeadYaw(headYaw);
            entity.setPitch(pitch);
            if (MathHelper.angleBetween(headYaw, entity.getYaw()) > 1.5F) {
                if (bodyToken == null || !bodyToken.active()) {
                    bodyToken = arg.get(BrainResource.BODY_CONTROL).orElse(null);
                }
                if (bodyToken != null && bodyToken.active()) {
                    final float bodyYaw;
                    final float lower = MathHelper.angleBetween(entity.getYaw(), MathHelper.wrapDegrees(headYaw - 1.5F));
                    final float upper = MathHelper.angleBetween(entity.getYaw(), MathHelper.wrapDegrees(headYaw + 1.5F));
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
    public void deinit(final BrainContext<T> context) {
        if (headToken != null && headToken.active()) {
            context.brain().resources().release(headToken);
        }
        if (bodyToken != null && bodyToken.active()) {
            context.brain().resources().release(bodyToken);
        }
    }

    private static Vec3d constantSpeedSlerp(final Vec3d start, final Vec3d end, final double stepSize) {
        final double omega = Math.acos(start.dotProduct(end));
        if (Math.abs(omega) < 0.00001) {
            return end;
        }
        final double t = Math.min(stepSize / omega, 1.0);
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

    public static <T extends LivingEntity> BrainNode<T, BasicTasks.Look.Result, BrainResourceRepository> dynamic(final BasicTasks.Look.Parameters parameters) {
        final MutableObject<Vec3d> last = new MutableObject<>(parameters.lookDir().normalize());
        final MutableDouble lastSpeed = new MutableDouble(parameters.lookSpeed());
        return BrainNodes.expectResult(new TaskTerminalBrainNode<>(BasicTasks.Look.KEY, (BiFunction<BrainResourceRepository, BrainContext<T>, BasicTasks.Look.Parameters>) (repository, context) -> parameters).resetOnContext((context, repository) -> {
            final Vec3d dir = parameters.lookDir().normalize();
            final double speed = parameters.lookSpeed();
            if (dir.dotProduct(last.getValue()) < 0.99 || Math.abs(speed - lastSpeed.doubleValue()) > 0.05) {
                last.setValue(dir);
                lastSpeed.setValue(speed);
                return true;
            }
            return false;
        }), () -> new RuntimeException("Look task factory error!"));
    }
}
