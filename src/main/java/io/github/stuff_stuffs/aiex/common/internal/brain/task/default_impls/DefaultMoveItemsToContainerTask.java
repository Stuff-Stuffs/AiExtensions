package io.github.stuff_stuffs.aiex.common.internal.brain.task.default_impls;

import io.github.stuff_stuffs.aiex.common.api.AiExApi;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.entity.inventory.NpcInventory;
import io.github.stuff_stuffs.aiex.common.api.util.InventorySlot;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public class DefaultMoveItemsToContainerTask<T extends Entity> implements BrainNode<T, BasicTasks.MoveItemsToContainer.Result, BrainResourceRepository> {
    private final BlockPos target;
    private final BasicTasks.MoveItemsToContainer.Filter filter;
    private final @Nullable Direction side;
    private final int frequency;
    private Object2LongMap<ItemVariant> moved;
    private Object2LongMap<ItemVariant> movedView;
    private long cooldown;

    public DefaultMoveItemsToContainerTask(final BlockPos target, final BasicTasks.MoveItemsToContainer.Filter filter, @Nullable final Direction side, final int frequency) {
        this.target = target;
        this.filter = filter;
        this.side = side;
        this.frequency = frequency;
    }

    @Override
    public void init(final BrainContext<T> context, final SpannedLogger logger) {
        cooldown = 0;
        moved = new Object2LongOpenHashMap<>();
        movedView = Object2LongMaps.unmodifiable(moved);
    }

    @Override
    public BasicTasks.MoveItemsToContainer.Result tick(final BrainContext<T> context, final BrainResourceRepository arg, final SpannedLogger logger) {
        if (--cooldown > 0) {
            return new BasicTasks.MoveItemsToContainer.Continue(movedView);
        }
        final Vec3d pos = context.entity().getEyePos();
        final double reachDistance = context.brain().config().get(BrainConfig.DEFAULT_REACH_DISTANCE);
        final VoxelShape shape = context.world().getBlockState(target).getOutlineShape(context.world(), target);
        final Optional<Vec3d> closest = shape.getClosestPointTo(pos.subtract(target.getX(), target.getY(), target.getZ()));
        if (closest.isEmpty()) {
            close(context);
            return new BasicTasks.MoveItemsToContainer.Error(BasicTasks.MoveItemsToContainer.ErrorType.MISSING_CONTAINER, movedView);
        }
        if (closest.get().add(target.getX(), target.getY(), target.getZ()).squaredDistanceTo(pos) > reachDistance * reachDistance) {
            close(context);
            return new BasicTasks.MoveItemsToContainer.Error(BasicTasks.MoveItemsToContainer.ErrorType.CANNOT_REACH, movedView);
        }
        final Storage<ItemVariant> storage = ItemStorage.SIDED.find(context.world(), target, side);
        if ((!(storage instanceof SlottedStorage<ItemVariant> slottedStorage))) {
            close(context);
            return new BasicTasks.MoveItemsToContainer.Error(BasicTasks.MoveItemsToContainer.ErrorType.MISSING_CONTAINER, movedView);
        }
        final NpcInventory inventory = AiExApi.NPC_INVENTORY.find(context.entity(), null);
        if (inventory == null) {
            throw new IllegalStateException();
        }
        open(context);
        for (final EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            final SingleSlotStorage<ItemVariant> slot = inventory.equipment().getSlot(equipmentSlot.getArmorStandSlotId());
            if (move(context, slot, BrainResource.ofEquipmentSlot(equipmentSlot), new InventorySlot(equipmentSlot), slottedStorage)) {
                return new BasicTasks.MoveItemsToContainer.Continue(movedView);
            }
        }
        final int count = inventory.main().getSlotCount();
        for (int i = 0; i < count; i++) {
            final SingleSlotStorage<ItemVariant> slot = inventory.main().getSlot(i);
            if (move(context, slot, BrainResource.ofInventorySlot(i), new InventorySlot(i), slottedStorage)) {
                return new BasicTasks.MoveItemsToContainer.Continue(movedView);
            }
        }
        return new BasicTasks.MoveItemsToContainer.Success(movedView);
    }

    private boolean move(final BrainContext<T> context, final SingleSlotStorage<ItemVariant> slot, final BrainResource brainResource, final InventorySlot inventorySlot, final SlottedStorage<ItemVariant> out) {
        final ItemVariant resource = slot.getResource();
        final BasicTasks.MoveItemsToContainer.Amount filtered = filter.filter(context, resource, slot.getAmount(), inventorySlot, out);
        if (filtered.amount() > 0) {
            final BrainResources.Token token = context.brain().resources().get(brainResource).orElse(null);
            if (token == null) {
                return false;
            }
            try (final Transaction transaction = Transaction.openOuter()) {
                final long maxInsert;
                final Storage<ItemVariant> target;
                if (filtered.target().isPresent()) {
                    final int index = filtered.target().getAsInt();
                    if (index < 0 || index >= out.getSlotCount()) {
                        return false;
                    }
                    target = out.getSlot(index);
                } else {
                    target = out;
                }
                try (final Transaction inner = transaction.openNested()) {
                    maxInsert = target.insert(resource, filtered.amount(), inner);
                    inner.abort();
                }
                final long toInsert = Math.min(maxInsert, filtered.amount());
                if (toInsert <= 0) {
                    transaction.abort();
                    return false;
                }
                final long extracted = slot.extract(resource, toInsert, transaction);
                if (extracted != toInsert) {
                    transaction.abort();
                    return false;
                }
                final long inserted = target.insert(resource, extracted, transaction);
                if (inserted != extracted) {
                    transaction.abort();
                    return false;
                }
                transaction.commit();
                final long prev = moved.getLong(resource);
                moved.put(resource, prev + inserted);
                cooldown = frequency;
                return true;
            } finally {
                context.brain().resources().release(token);
            }
        }
        return false;
    }

    @Override
    public void deinit(final BrainContext<T> context, final SpannedLogger logger) {
        close(context);
    }

    private void open(final BrainContext<?> context) {
        if (!context.hasPlayerDelegate()) {
            return;
        }
        final Inventory inventory;
        final BlockEntity entity = context.world().getBlockEntity(target);
        if (entity == null) {
            return;
        }
        if (entity instanceof Inventory) {
            inventory = (Inventory) entity;
        } else {
            return;
        }
        if (context.playerDelegate().openInventory == null || context.playerDelegate().openInventory.get() != inventory) {
            inventory.onOpen(context.playerDelegate());
            close(context);
            context.playerDelegate().openInventory = new WeakReference<>(inventory);
        }
    }

    private void close(final BrainContext<?> context) {
        if (!context.hasPlayerDelegate()) {
            return;
        }
        final WeakReference<Inventory> reference = context.playerDelegate().openInventory;
        if (reference == null) {
            return;
        }
        final Inventory inventory = reference.get();
        if (inventory == null) {
            return;
        }
        inventory.onClose(context.playerDelegate());
        context.playerDelegate().openInventory = null;
    }
}
