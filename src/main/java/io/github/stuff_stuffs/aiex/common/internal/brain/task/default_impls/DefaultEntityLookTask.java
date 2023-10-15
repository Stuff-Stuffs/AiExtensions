package io.github.stuff_stuffs.aiex.common.internal.brain.task.default_impls;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.brain.task.flow.ContextResetTask;
import io.github.stuff_stuffs.aiex.common.api.brain.task.Task;
import io.github.stuff_stuffs.aiex.common.api.brain.task.Tasks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DefaultEntityLookTask<T extends Entity> implements Task<BasicTasks.Look.Result, T> {
    private final Entity target;
    private final RaycastContext.ShapeType shapeType;
    private final double lookSpeed;
    private Vec3d lastEyePos;
    private Vec3d lastTargetPos;
    private Task<BasicTasks.Look.Result, T> delegate = null;

    public DefaultEntityLookTask(final Entity target, final RaycastContext.ShapeType type, final double speed) {
        this.target = target;
        shapeType = type;
        lookSpeed = speed;
    }

    @Override
    public BasicTasks.Look.Result run(final BrainContext<T> context) {
        final T entity = context.entity();
        final Vec3d eyePos = entity.getPos().add(0, entity.getEyeHeight(entity.getPose()), 0);
        final Box box = target.getBoundingBox();
        if (box.contains(eyePos)) {
            return BasicTasks.Look.Result.ALIGNED;
        }
        final Vec3d otherEye;
        if (target instanceof LivingEntity o) {
            otherEye = o.getEyePos();
        } else {
            otherEye = box.getCenter();
        }
        if (lastEyePos == null || lastEyePos.squaredDistanceTo(eyePos) > 0.01) {
            lastEyePos = eyePos;
            if (delegate != null) {
                delegate.stop(context);
            }
            delegate = null;
        }
        if (lastTargetPos == null || lastTargetPos.squaredDistanceTo(otherEye) > 0.01) {
            lastTargetPos = otherEye;
            if (delegate != null) {
                delegate.stop(context);
            }
            delegate = null;
        }
        if (delegate == null) {
            final Vec3d target = computeTarget(eyePos, box, otherEye, context.world(), shapeType, context.entity(), context.randomSeed());
            if (target == null) {
                delegate = Tasks.constant(BasicTasks.Look.Result.FAILED);
            } else {
                final Vec3d delta = target.subtract(eyePos);
                final Optional<Task<BasicTasks.Look.Result, T>> task = context.createTask(BasicTasks.Look.KEY, new BasicTasks.Look.Parameters() {
                    @Override
                    public Vec3d lookDir() {
                        return delta;
                    }

                    @Override
                    public double lookSpeed() {
                        return lookSpeed;
                    }
                });
                delegate = task.orElseGet(() -> Tasks.constant(BasicTasks.Look.Result.FAILED));
            }
        }
        return delegate.run(context);
    }

    public static @Nullable Vec3d computeTarget(final Vec3d eyePos, final Box box, final Vec3d otherEye, final World world, final RaycastContext.ShapeType type, final Entity entity, final long seed) {
        final Vec3d target;
        final double distSq = distSqToBox(eyePos, box);
        if (distSq < 0.01 || distSq > 32.0 * 32.0) {
            target = otherEye;
        } else {
            final Random random = new Xoroshiro128PlusPlusRandom(seed);
            final double dx = box.maxX - box.minX;
            final double dy = box.maxY - box.minY;
            final double dz = box.maxZ - box.minZ;
            Vec3d best = null;
            double bestDist = Double.POSITIVE_INFINITY;
            for (int i = 0; i < 16; i++) {
                final double x = box.minX + dx * random.nextDouble();
                final double y = box.minY + dy * random.nextDouble();
                final double z = box.minZ + dz * random.nextDouble();
                final Vec3d t = new Vec3d(x, y, z);
                final BlockHitResult result = world.raycast(new RaycastContext(eyePos, t, type, RaycastContext.FluidHandling.NONE, entity));
                if (result.getType() == HitResult.Type.MISS) {
                    final double d = otherEye.squaredDistanceTo(t);
                    if (best == null || d < bestDist) {
                        best = t;
                        bestDist = d;
                    }
                }
            }
            target = best;
        }
        return target;
    }

    public static double distSqToBox(final Vec3d v, final Box box) {
        final double x = MathHelper.clamp(v.x, box.minX, box.maxX);
        final double y = MathHelper.clamp(v.y, box.minY, box.maxY);
        final double z = MathHelper.clamp(v.z, box.minZ, box.maxZ);
        return v.squaredDistanceTo(x, y, z);
    }

    @Override
    public void stop(final BrainContext<T> context) {
        if (delegate != null) {
            delegate.stop(context);
        }
    }

    public static Task<BasicTasks.Look.Result, Entity> dynamic(final BasicTasks.Look.EntityParameters parameters) {
        final MutableObject<Entity> lastTarget = new MutableObject<>(parameters.entity());
        final MutableObject<RaycastContext.ShapeType> lastShapeType = new MutableObject<>(parameters.type());
        final MutableDouble lastLookSpeed = new MutableDouble(parameters.lookSpeed());
        return Tasks.expect(new ContextResetTask<>(ctx -> ctx.createTask(BasicTasks.Look.ENTITY_KEY, parameters).orElse(null), context -> {
            boolean reset = false;
            if (lastTarget.getValue() != parameters.entity()) {
                reset = true;
            } else if (lastShapeType.getValue() != parameters.type()) {
                reset = true;
            } else if (Math.abs(lastLookSpeed.doubleValue() - parameters.lookSpeed()) > 0.05) {
                reset = true;
            }
            if (reset) {
                lastTarget.setValue(parameters.entity());
                lastShapeType.setValue(parameters.type());
                lastLookSpeed.setValue(parameters.lookSpeed());
            }
            return reset;
        }), RuntimeException::new);
    }
}
