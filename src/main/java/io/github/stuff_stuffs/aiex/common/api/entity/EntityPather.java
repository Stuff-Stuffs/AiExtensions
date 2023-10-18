package io.github.stuff_stuffs.aiex.common.api.entity;

import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public interface EntityPather {
    boolean startFollowingPath(Target target, double error, double maxCost, boolean partial, double urgency);

    void setUrgency(double urgency);

    double urgency();

    boolean idle();

    interface EntityNode<N extends EntityNode<N>> {
        int x();

        int y();

        int z();

        double cost();

        N previous();
    }

    interface EntityContext {
        Entity entity();

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
