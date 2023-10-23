package io.github.stuff_stuffs.aiex.common.api.entity.pathing;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.ShapeCache;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public interface EntityPather {
    default boolean startFollowingPath(final Target target, final double error, final double maxPathLength, final boolean partial, final double urgency) {
        return startFollowingPath(target, error, maxPathLength, partial, urgency, false);
    }

    boolean startFollowingPath(Target target, double error, double maxPathLength, boolean partial, double urgency, boolean immediate);

    boolean idle();

    void tick();

    interface EntityContext {
        LivingEntity entity();

        double maxPathLength();

        ShapeCache cache();
    }

    sealed interface Target {
    }

    record SingleTarget(Vec3d target) implements Target {
    }

    non-sealed abstract class MetricTarget implements Target {
        public abstract double score(int x, int y, int z, EntityContext context);
    }
}
