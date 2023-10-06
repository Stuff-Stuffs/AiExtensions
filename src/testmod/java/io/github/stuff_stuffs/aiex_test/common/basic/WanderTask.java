package io.github.stuff_stuffs.aiex_test.common.basic;

import net.minecraft.util.math.Vec3d;

public interface WanderTask {
    enum Result {
        SUCCESS,
        FAILED
    }

    interface Parameters {
        Vec3d center();

        double range();
    }
}
