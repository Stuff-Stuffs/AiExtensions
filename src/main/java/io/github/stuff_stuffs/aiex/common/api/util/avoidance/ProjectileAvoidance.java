package io.github.stuff_stuffs.aiex.common.api.util.avoidance;

import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public final class ProjectileAvoidance {
    private static final Vec3d UP = new Vec3d(0, 1, 0);

    private ProjectileAvoidance() {
    }

    public static DangerScorer projectileAvoidance(final Vec3d pos, final Vec3d velocity, final double buffer, final UUID id) {
        final Vec3d up;
        final double speed = velocity.length();
        if (speed < 0.001) {
            return new DangerScorer(id) {
                @Override
                public double score(final Vec3d pos) {
                    return 0.0;
                }
            };
        }
        final Vec3d normVel = velocity.multiply(1 / speed);
        if (Math.abs(normVel.dotProduct(UP)) > 0.99) {
            up = normVel.crossProduct(new Vec3d(0, 0, -1));
        } else {
            up = normVel.crossProduct(UP);
        }
        final Vec3d left = normVel.crossProduct(up);
        return new DangerScorer(id) {
            @Override
            public double score(final Vec3d targetPos) {
                final Vec3d translated = targetPos.subtract(pos);
                final double x = translated.dotProduct(up);
                final double y = translated.dotProduct(left);
                final double z = translated.dotProduct(normVel);
                if (z < -buffer) {
                    return 0;
                }
                final double scaleComponent = -1 / (z + buffer + 1) + 1;
                final double tubeComponent = Math.sqrt(x * x + y * y) - buffer;
                final double euclidComponent = (targetPos.distanceTo(pos) - buffer) / (speed * 8 + 1);
                return (tubeComponent + euclidComponent) * scaleComponent;
            }
        };
    }

    public static abstract class DangerScorer {
        public final UUID id;

        protected DangerScorer(final UUID id) {
            this.id = id;
        }

        public abstract double score(Vec3d pos);
    }
}
