package io.github.stuff_stuffs.aiex_test.common.basic;

import io.github.stuff_stuffs.aiex.common.api.brain.task.Task;
import net.minecraft.util.math.Vec3d;

public interface WalkTask<C> extends Task<WalkTask.Result, C> {
    enum Result {
        CONTINUE,
        DONE,
        FAILED
    }

    interface Parameters {
        Vec3d target();

        double maxError();
    }

    interface Navigator {
        boolean isPathing();

        boolean walkTo(Vec3d pos, double error);
    }
}
