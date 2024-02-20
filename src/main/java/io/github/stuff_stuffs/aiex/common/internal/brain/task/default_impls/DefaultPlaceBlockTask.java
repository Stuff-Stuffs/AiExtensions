package io.github.stuff_stuffs.aiex.common.internal.brain.task.default_impls;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.BasicMemories;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.TickCountdownMemory;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
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

public class DefaultPlaceBlockTask<T extends LivingEntity> implements BrainNode<T, BasicTasks.PlaceBlock.Result, BrainResourceRepository> {
    private final BlockPos pos;
    private final Predicate<BlockState> targetState;

    public DefaultPlaceBlockTask(final BasicTasks.PlaceBlock.Parameters parameters) {
        pos = parameters.pos().toImmutable();
        targetState = parameters.targetState();
    }

    @Override
    public void init(final BrainContext<T> context, final SpannedLogger logger) {

    }

    @Override
    public BasicTasks.PlaceBlock.Result tick(final BrainContext<T> context, final BrainResourceRepository arg, final SpannedLogger logger) {
        final int cooldownTicks = context.brain().memories().get(BasicMemories.USE_ITEM_COOLDOWN_TICKS).map(memory -> memory.get().ticksRemaining()).orElse(0);
        if (cooldownTicks > 0) {
            return new BasicTasks.PlaceBlock.Cooldown(cooldownTicks);
        }
        final Vec3d eyePos = context.entity().getEyePos();
        final double distanceSq = AiExMathUtil.boxDistanceSq(eyePos.x, eyePos.y, eyePos.z, new Box(pos));
        final double reachDistance = context.brain().config().get(BrainConfig.DEFAULT_REACH_DISTANCE);
        if (distanceSq > reachDistance * reachDistance) {
            return new BasicTasks.PlaceBlock.Error(BasicTasks.PlaceBlock.ErrorType.CANT_REACH);
        }
        BlockState state = context.world().getBlockState(pos);
        if (!state.isAir()) {
            return new BasicTasks.PlaceBlock.SpaceOccupied(state);
        }
        final BrainResources.Token token = arg.get(BrainResource.ACTIVE_MAIN_HAND_CONTROL).orElse(null);
        if (token == null) {
            return new BasicTasks.PlaceBlock.Error(BasicTasks.PlaceBlock.ErrorType.RESOURCE_ACQUISITION);
        }
        final ItemStack stack = context.entity().getMainHandStack();
        //noinspection unchecked
        final NpcBlockPlacementHandler<T> handler = NpcBlockPlacementHandlers.get((Class<T>) context.entity().getClass());
        final boolean handled = handler.handle(stack.getItem(), context, pos, targetState);
        context.brain().resources().release(token);
        if (!handled) {
            return new BasicTasks.PlaceBlock.Error(BasicTasks.PlaceBlock.ErrorType.UNKNOWN_ITEM);
        }
        context.brain().memories().put(BasicMemories.USE_ITEM_COOLDOWN_TICKS, new TickCountdownMemory(4));
        state = context.world().getBlockState(pos);
        if (!targetState.test(state)) {
            return new BasicTasks.PlaceBlock.UnexpectedState(state);
        }
        return new BasicTasks.PlaceBlock.Success(state);
    }

    @Override
    public void deinit(final BrainContext<T> context, final SpannedLogger logger) {

    }
}
