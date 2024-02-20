package io.github.stuff_stuffs.aiex_test.common.basic;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.BrainNodes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public final class BasicTestBrainNodes {
    public static <C extends Entity> BrainNode<C, Optional<Vec3d>, Unit> nearestPlayer() {
        return BrainNodes.terminal((context, unit) -> {
            final PlayerEntity player = context.entity().getEntityWorld().getClosestPlayer(context.entity(), 128.0);
            if (player != null) {
                return Optional.of(player.getPos());
            }
            return Optional.empty();
        });
    }

    private BasicTestBrainNodes() {
    }
}
