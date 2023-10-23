package io.github.stuff_stuffs.aiex.common.api.util;

import net.minecraft.util.math.Box;

public final class AiExMathUtil {
    //https://www.desmos.com/calculator/bbgs3z1ruf
    public static double adjustableAsymptote(final double x, final double asymptote, final double intercept, final double scale) {
        final double delta = asymptote - x;
        return intercept - scale / (asymptote * asymptote) + scale / (delta * delta);
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
