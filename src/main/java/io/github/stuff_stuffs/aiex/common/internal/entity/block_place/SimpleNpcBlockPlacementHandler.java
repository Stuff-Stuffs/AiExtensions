package io.github.stuff_stuffs.aiex.common.internal.entity.block_place;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.entity.block_place.NpcBlockPlacementHandler;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Set;
import java.util.function.Predicate;

public class SimpleNpcBlockPlacementHandler<T> implements NpcBlockPlacementHandler<T> {
    private final Set<Item> handled;

    public SimpleNpcBlockPlacementHandler(final Set<Item> handled) {
        this.handled = handled;
    }

    @Override
    public boolean handle(final Item item, final BrainContext<T> context, final BlockPos pos, final Predicate<BlockState> targetState) {
        if (!handled.contains(item) || !context.hasPlayerDelegate() || !(item instanceof BlockItem blockItem)) {
            return false;
        }
        if (!targetState.test(blockItem.getBlock().getDefaultState())) {
            return false;
        }
        final ItemUsageContext usageContext = new ItemUsageContext(context.playerDelegate(), Hand.MAIN_HAND, NpcBlockPlacementHandler.castAt(pos, Direction.NORTH, 0.5, 0.5));
        return item.useOnBlock(usageContext).isAccepted();
    }

    @Override
    public Set<Item> handles() {
        return handled;
    }
}
