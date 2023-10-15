package io.github.stuff_stuffs.aiex.common.internal.brain.task.default_impls;

import io.github.stuff_stuffs.aiex.common.api.AiExApi;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.brain.task.Task;
import io.github.stuff_stuffs.aiex.common.api.entity.inventory.NpcInventory;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class DefaultSwapStackTask<T extends Entity> implements Task<BasicTasks.SwapStack.Result, T> {
    private final BasicTasks.SwapStack.Parameters parameters;
    private @Nullable BrainResources.Token sourceToken = null;
    private @Nullable BrainResources.Token destinationToken = null;

    public DefaultSwapStackTask(final BasicTasks.SwapStack.Parameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public BasicTasks.SwapStack.Result run(final BrainContext<T> context) {
        final NpcInventory inventory = AiExApi.NPC_INVENTORY.find(context.entity(), null);
        if (inventory == null) {
            throw new IllegalStateException();
        }
        if (sourceToken == null || !sourceToken.active()) {
            sourceToken = context.brain().resources().get(parameters.source().map(BrainResource::ofInventorySlot, BrainResource::ofEquipmentSlot)).orElse(null);
            if (sourceToken == null) {
                return BasicTasks.SwapStack.Result.RESOURCE_ACQUISITION_ERROR;
            }
        }
        if (destinationToken == null || !destinationToken.active()) {
            destinationToken = context.brain().resources().get(parameters.destination().map(BrainResource::ofInventorySlot, BrainResource::ofEquipmentSlot)).orElse(null);
            if (sourceToken == null) {
                return BasicTasks.SwapStack.Result.RESOURCE_ACQUISITION_ERROR;
            }
        }
        try (final Transaction transaction = Transaction.openOuter()) {
            final SingleSlotStorage<ItemVariant> source = parameters.source().map(i -> inventory.main().getSlot(i), slot -> inventory.equipment().getSlot(slot.getArmorStandSlotId()));
            final SingleSlotStorage<ItemVariant> destination = parameters.destination().map(i -> inventory.main().getSlot(i), slot -> inventory.equipment().getSlot(slot.getArmorStandSlotId()));
            final ItemVariant sourceItem = source.getResource();
            final ItemVariant destinationItem = destination.getResource();
            final long takenSource = source.extract(sourceItem, source.getAmount(), transaction);
            final long takenDestination = destination.extract(destinationItem, destination.getAmount(), transaction);
            if (takenSource != destination.insert(sourceItem, takenSource, transaction) || takenDestination != source.insert(destinationItem, takenDestination, transaction)) {
                transaction.abort();
                return BasicTasks.SwapStack.Result.ERROR;
            } else {
                transaction.commit();
                return BasicTasks.SwapStack.Result.OK;
            }
        }
    }

    @Override
    public void stop(final BrainContext<T> context) {
        if (sourceToken == null || !sourceToken.active()) {
            context.brain().resources().release(sourceToken);
            sourceToken = null;
        }
        if (destinationToken == null || !destinationToken.active()) {
            context.brain().resources().release(destinationToken);
            destinationToken = null;
        }
    }
}
