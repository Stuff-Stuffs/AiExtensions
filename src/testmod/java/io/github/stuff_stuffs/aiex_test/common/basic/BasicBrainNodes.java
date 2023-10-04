package io.github.stuff_stuffs.aiex_test.common.basic;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.TaskTerminalBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskKey;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.function.Function;

public final class BasicBrainNodes {
    public static <C extends Entity> BrainNode<C, Optional<Vec3d>, Unit> nearestPlayer() {
        return new BrainNode<>() {
            @Override
            public void init(final BrainContext<C> context) {

            }

            @Override
            public Optional<Vec3d> tick(final BrainContext<C> context, final Unit arg) {
                final PlayerEntity player = context.entity().getEntityWorld().getClosestPlayer(context.entity(), 128.0);
                if (player != null) {
                    return Optional.of(player.getPos());
                }
                return Optional.empty();
            }

            @Override
            public void deinit() {

            }
        };
    }

    public static <C> BrainNode<C, TaskTerminalBrainNode.Result<WalkTask.Result>, Vec3d> walk(final TaskKey<WalkTask.Result, WalkTask.Parameters> key, final double maxError) {
        return walk(key, vec3d -> new WalkTask.Parameters() {
            @Override
            public Vec3d target() {
                return vec3d;
            }

            @Override
            public double maxError() {
                return maxError;
            }
        });
    }

    public static <C, FC> BrainNode<C, TaskTerminalBrainNode.Result<WalkTask.Result>, FC> walk(final TaskKey<WalkTask.Result, WalkTask.Parameters> key, final Function<FC, WalkTask.Parameters> parameterFactory) {
        return new TaskTerminalBrainNode<>(key, (ctx, context) -> parameterFactory.apply(ctx));
    }

    private BasicBrainNodes() {
    }
}
