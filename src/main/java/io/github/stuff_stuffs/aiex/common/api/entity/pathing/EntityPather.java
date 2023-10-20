package io.github.stuff_stuffs.aiex.common.api.entity.pathing;

import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public interface EntityPather {
    boolean startFollowingPath(Target target, double error, double maxPathLength, boolean partial, double urgency);

    void setUrgency(double urgency);

    double urgency();

    boolean idle();

    interface EntityContext {
        Entity entity();

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
