package io.github.stuff_stuffs.aiex.common.internal.entity.block_place;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.entity.block_place.NpcBlockPlacementHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Set;
import java.util.function.Predicate;

public class PillarNpcBlockPlacementHandler<T> implements NpcBlockPlacementHandler<T> {
    private final Set<Item> handled;

    public PillarNpcBlockPlacementHandler(final Set<Item> handled) {
        this.handled = handled;
    }

    @Override
    public boolean handle(final Item item, final BrainContext<? extends T> context, final BlockPos pos, final Predicate<BlockState> targetState) {
        if (!handled.contains(item) || !context.hasPlayerDelegate() || !(item instanceof BlockItem blockItem)) {
            return false;
        }
        final Block block = blockItem.getBlock();
        if (!block.getStateManager().getProperties().contains(Properties.AXIS)) {
            return false;
        }
        for (final Direction.Axis axis : Direction.Axis.values()) {
            if (targetState.test(block.getDefaultState().with(Properties.AXIS, axis))) {
                final ItemUsageContext usageContext = new ItemUsageContext(context.playerDelegate(), Hand.MAIN_HAND, NpcBlockPlacementHandler.castAt(pos, Direction.from(axis, Direction.AxisDirection.POSITIVE), 0.5, 0.5));
                if (item.useOnBlock(usageContext).isAccepted()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Set<Item> handles() {
        return handled;
    }
}
