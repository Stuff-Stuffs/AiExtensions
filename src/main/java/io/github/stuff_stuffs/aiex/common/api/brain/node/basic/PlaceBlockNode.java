package io.github.stuff_stuffs.aiex.common.api.brain.node.basic;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import io.github.stuff_stuffs.aiex.common.api.entity.block_place.NpcBlockPlacementHandler;
import io.github.stuff_stuffs.aiex.common.api.entity.block_place.NpcBlockPlacementHandlers;
import io.github.stuff_stuffs.aiex.common.api.util.AiExMathUtil;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

public class PlaceBlockNode<T extends LivingEntity> implements BrainNode<T, PlaceBlockNode.Result, PlaceBlockNode.Argument> {
    private int cooldownTicks = 0;

    @Override
    public void init(final BrainContext<T> context, final SpannedLogger logger) {

    }

    @Override
    public Result tick(final BrainContext<T> context, final Argument arg, final SpannedLogger logger) {
        cooldownTicks--;
        final Vec3d eyePos = context.entity().getEyePos();
        final double distanceSq = AiExMathUtil.boxDistanceSq(eyePos.x, eyePos.y, eyePos.z, new Box(arg.pos()));
        final double reachDistance = context.brain().config().get(BrainConfig.DEFAULT_REACH_DISTANCE);
        if (distanceSq > reachDistance * reachDistance) {
            return new Error(ErrorType.CANT_REACH);
        }
        if (cooldownTicks > 0) {
            return new Cooldown(cooldownTicks);
        }
        BlockState state = context.world().getBlockState(arg.pos());
        if (!state.isAir()) {
            return new SpaceOccupied(state);
        }
        final BrainResources.Token token = arg.repository().get(BrainResource.MAIN_HAND_CONTROL).orElse(null);
        if (token == null) {
            return new Error(ErrorType.RESOURCE_ACQUISITION);
        }
        final ItemStack stack = context.entity().getMainHandStack();
        //noinspection unchecked
        final NpcBlockPlacementHandler<T> handler = NpcBlockPlacementHandlers.get((Class<T>) context.entity().getClass());
        final boolean handled = handler.handle(stack.getItem(), context, arg.pos(), arg.target());
        context.brain().resources().release(token);
        if (!handled) {
            return new Error(ErrorType.UNKNOWN_ITEM);
        }
        cooldownTicks = 4;
        state = context.world().getBlockState(arg.pos());
        if (!arg.target().test(state)) {
            return new UnexpectedState(state);
        }
        return new Success(state);
    }

    @Override
    public void deinit(final BrainContext<T> context, final SpannedLogger logger) {

    }

    public interface Argument {
        BrainResourceRepository repository();

        BlockPos pos();

        Predicate<BlockState> target();
    }

    public sealed interface Result {
    }

    public record Error(ErrorType type) implements Result {
    }

    public record Cooldown(int tickRemaining) implements Result {
    }

    public record Success(BlockState state) implements Result {
    }

    public record UnexpectedState(BlockState state) implements Result {
    }

    public record SpaceOccupied(BlockState state) implements Result {
    }

    public enum ErrorType {
        CANT_REACH,
        RESOURCE_ACQUISITION,
        UNKNOWN_ITEM
    }
}
