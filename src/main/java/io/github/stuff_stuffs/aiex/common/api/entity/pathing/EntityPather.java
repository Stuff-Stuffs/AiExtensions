package io.github.stuff_stuffs.aiex.common.api.entity.pathing;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.ShapeCache;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestBounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public interface EntityPather {
    default boolean startFollowingPath(final Target target, final double error, final double maxPathLength, final boolean partial, final double urgency) {
        return startFollowingPath(target, error, maxPathLength, partial, urgency, false);
    }

    boolean startFollowingPath(Target target, double error, double maxPathLength, boolean partial, double urgency, boolean immediate);

    boolean idle();

    void tick();

    void stop();

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

        public static MetricTarget ofBounds(final AreaOfInterestBounds bounds) {
            return new MetricTarget() {
                @Override
                public double score(final int x, final int y, final int z, final EntityContext context) {
                    final int dx = Math.abs(x - Math.max(Math.min(x, bounds.maxX()), bounds.minX()));
                    final int dy = Math.abs(y - Math.max(Math.min(y, bounds.maxY()), bounds.minY()));
                    final int dz = Math.abs(z - Math.max(Math.min(z, bounds.maxZ()), bounds.minZ()));
                    return dx + dy + dz;
                }
            };
        }
    }
}
