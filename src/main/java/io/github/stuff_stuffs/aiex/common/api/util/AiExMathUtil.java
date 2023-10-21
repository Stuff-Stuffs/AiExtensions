package io.github.stuff_stuffs.aiex.common.api.util;

public final class AiExMathUtil {
    //https://www.desmos.com/calculator/bbgs3z1ruf
    public static double adjustableAsymptote(final double x, final double asymptote, final double intercept, final double scale) {
        final double delta = asymptote - x;
        return intercept - scale / (asymptote * asymptote) + scale / (delta * delta);
    }

    private AiExMathUtil() {
    }
}
