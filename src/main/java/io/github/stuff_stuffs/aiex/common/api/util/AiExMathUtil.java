package io.github.stuff_stuffs.aiex.common.api.util;

import net.minecraft.util.math.Box;

public final class AiExMathUtil {
    public static double fallCost(final double health, final double maxHealth, final double damage) {
        if (damage >= health - 0.1) {
            return Double.POSITIVE_INFINITY;
        }
        final double scale = 4000;
        final double scoreCurrent = 1 / health - 1 / maxHealth;
        final double scoreNext = 1 / (health - damage) - 1 / maxHealth;
        return scale * (scoreNext - scoreCurrent);
    }

    public static double boxDistanceSq(final double x, final double y, final double z, final Box box) {
        final double rx = Math.max(Math.min(x, box.maxX), box.minX);
        final double ry = Math.max(Math.min(y, box.maxY), box.minY);
        final double rz = Math.max(Math.min(z, box.maxZ), box.minZ);
        final double dx = x - rx;
        final double dy = y - ry;
        final double dz = z - rz;
        return dx * dx + dy * dy + dz * dz;
    }

    private AiExMathUtil() {
    }
}
