package io.github.stuff_stuffs.aiex.common.api.entity;

import net.minecraft.util.math.Vec3d;

public interface EntityNavigator {
    boolean walkTo(Vec3d pos, double error);
}
