package io.github.stuff_stuffs.aiex.common.internal.brain.task.default_impls;

import io.github.stuff_stuffs.aiex.common.api.AiExApi;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.entity.inventory.NpcInventory;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class DefaultSwapStackTask<T extends Entity> implements BrainNode<T, BasicTasks.SwapStack.Result, BrainResourceRepository> {
    private final BasicTasks.SwapStack.Parameters parameters;
    private @Nullable BrainResources.Token sourceToken = null;
    private @Nullable BrainResources.Token destinationToken = null;

    public DefaultSwapStackTask(final BasicTasks.SwapStack.Parameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public void init(final BrainContext<T> context, final SpannedLogger logger) {

    }

    @Override
    public BasicTasks.SwapStack.Result tick(final BrainContext<T> context, final BrainResourceRepository arg, final SpannedLogger logger) {
        try (final var l = logger.open("DefaultSwapStack")) {
            final NpcInventory inventory = AiExApi.NPC_INVENTORY.find(context.entity(), null);
            if (inventory == null) {
                throw new IllegalStateException();
            }
            if (sourceToken == null || !sourceToken.active()) {
                l.debug("Trying to acquire source token");
                sourceToken = arg.get(parameters.source().resource()).orElse(null);
                if (sourceToken == null) {
                    l.debug("Failed!");
                    return BasicTasks.SwapStack.Result.RESOURCE_ACQUISITION_ERROR;
                }
                l.debug("Success");
            }
            if (destinationToken == null || !destinationToken.active()) {
                l.debug("Trying to acquire destination token");
                destinationToken = arg.get(parameters.destination().resource()).orElse(null);
                if (sourceToken == null) {
                    l.debug("Failed!");
                    return BasicTasks.SwapStack.Result.RESOURCE_ACQUISITION_ERROR;
                }
                l.debug("Success");
            }
            try (final Transaction transaction = Transaction.openOuter()) {
                final SingleSlotStorage<ItemVariant> source = parameters.source().slot(inventory);
                final SingleSlotStorage<ItemVariant> destination = parameters.destination().slot(inventory);
                final ItemVariant sourceItem = source.getResource();
                final ItemVariant destinationItem = destination.getResource();
                final long takenSource = source.extract(sourceItem, source.getAmount(), transaction);
                final long takenDestination = destination.extract(destinationItem, destination.getAmount(), transaction);
                if (takenSource != destination.insert(sourceItem, takenSource, transaction) || takenDestination != source.insert(destinationItem, takenDestination, transaction)) {
                    l.warning("Aborted swapping stacks!");
                    transaction.abort();
                    return BasicTasks.SwapStack.Result.ERROR;
                } else {
                    l.debug("Succeeded swapping stacks!");
                    transaction.commit();
                    return BasicTasks.SwapStack.Result.OK;
                }
            }
        }
    }

    @Override
    public void deinit(final BrainContext<T> context, final SpannedLogger logger) {
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
