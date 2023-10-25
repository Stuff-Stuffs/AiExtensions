package io.github.stuff_stuffs.aiex.common.internal.entity;

import io.github.stuff_stuffs.aiex.common.api.entity.inventory.NpcInventory;
import io.github.stuff_stuffs.aiex.common.api.util.InventorySlot;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.TagKey;

@SuppressWarnings("UnstableApiUsage")
public class DelegatingPlayerInventory extends PlayerInventory {
    private final NpcInventory delegate;

    public DelegatingPlayerInventory(final PlayerEntity player, final NpcInventory delegate) {
        super(player);
        this.delegate = delegate;
    }

    private boolean canStackAddMore(final ItemStack existingStack, final ItemStack stack) {
        return !existingStack.isEmpty()
                && ItemStack.canCombine(existingStack, stack)
                && existingStack.isStackable()
                && existingStack.getCount() < existingStack.getMaxCount()
                && existingStack.getCount() < getMaxCountPerStack();
    }

    private int addStack(final ItemStack stack) {
        int i = getOccupiedSlotWithRoomForStack(stack);
        if (i == -1) {
            i = getEmptySlot();
        }

        return i == -1 ? stack.getCount() : addStack(i, stack);
    }

    private int addStack(final int slot, final ItemStack stack) {
        int i = stack.getCount();
        final ItemStack itemStack = getStack(slot);
        if (itemStack.isEmpty()) {
            setStack(slot, stack);
            return 0;
        }

        int j = Math.min(i, itemStack.getMaxCount() - itemStack.getCount());

        if (j > getMaxCountPerStack() - itemStack.getCount()) {
            j = getMaxCountPerStack() - itemStack.getCount();
        }

        if (j == 0) {
            return i;
        } else {
            i -= j;
            itemStack.increment(j);
            setStack0(slot, itemStack);
            return i;
        }
    }

    @Override
    public int getEmptySlot() {
        if (delegate.equipment().getSlot(EquipmentSlot.MAINHAND.getArmorStandSlotId()).getResource().isBlank()) {
            return 0;
        }
        final int count = delegate.main().getSlotCount();
        for (int i = 0; i < count; i++) {
            if (delegate.main().getSlot(i).getResource().isBlank()) {
                return i + 1;
            }
        }
        return -1;
    }

    @Override
    public ItemStack getMainHandStack() {
        final SingleSlotStorage<ItemVariant> slot = delegate.equipment().getSlot(EquipmentSlot.MAINHAND.getArmorStandSlotId());
        return slot.getResource().toStack((int) slot.getAmount());
    }

    @Override
    public int getSlotWithStack(final ItemStack stack) {
        final int count = delegate.main().getSlotCount();
        for (int i = 0; i < count; ++i) {
            if (!delegate.main().getSlot(i).getResource().isBlank() && ItemStack.canCombine(stack, getStack(i))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getOccupiedSlotWithRoomForStack(final ItemStack stack) {
        if (canStackAddMore(getStack(0), stack)) {
            return 0;
        } else if (canStackAddMore(getStack(40), stack)) {
            return 40;
        } else {
            final int size = delegate.main().getSlotCount();
            for (int i = 0; i < size; ++i) {
                if (canStackAddMore(getStack(i), stack)) {
                    return i;
                }
            }
            return -1;
        }
    }

    private boolean setStack0(final int index, final ItemStack stack) {
        final InventorySlot mapped = map(index);
        try (final Transaction transaction = Transaction.openOuter()) {
            final SingleSlotStorage<ItemVariant> slot = mapped.slot(delegate);
            final long amount = slot.getAmount();
            if (amount != 0) {
                final long extracted = slot.extract(slot.getResource(), amount, transaction);
                if (extracted != amount) {
                    transaction.abort();
                    return false;
                }
            }
            if (stack.getItem() != Items.AIR && stack.getCount() > 0) {
                if (slot.insert(ItemVariant.of(stack.getItem(), stack.getNbt()), stack.getCount(), transaction) != stack.getCount()) {
                    transaction.abort();
                    return false;
                }
            }
            transaction.commit();
            return true;
        }
    }

    @Override
    public boolean insertStack(int slot, final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            if (stack.isDamaged()) {
                if (slot == -1) {
                    slot = getEmptySlot();
                }

                if (slot >= 0) {
                    setStack0(slot, stack);
                    return true;
                } else if (player.getAbilities().creativeMode) {
                    stack.setCount(0);
                    return true;
                } else {
                    return false;
                }
            } else {
                int i;
                do {
                    i = stack.getCount();
                    if (slot == -1) {
                        stack.setCount(addStack(stack));
                    } else {
                        stack.setCount(addStack(slot, stack));
                    }
                } while (!stack.isEmpty() && stack.getCount() < i);

                if (stack.getCount() == i && player.getAbilities().creativeMode) {
                    stack.setCount(0);
                    return true;
                } else {
                    return stack.getCount() < i;
                }
            }
        }
    }

    @Override
    public int size() {
        return delegate.main().getSlotCount() + delegate.equipment().getSlotCount();
    }

    @Override
    public boolean isEmpty() {
        final int count = delegate.main().getSlotCount();
        for (int i = 0; i < count; i++) {
            if (!delegate.main().getSlot(i).getResource().isBlank()) {
                return false;
            }
        }
        final EquipmentSlot[] values = EquipmentSlot.values();
        for (final EquipmentSlot value : values) {
            if (!delegate.equipment().getSlot(value.getArmorStandSlotId()).getResource().isBlank()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean contains(final ItemStack stack) {
        final int size = size();
        for (int i = 0; i < size; i++) {
            final ItemStack itemStack = getStack(i);
            if (!itemStack.isEmpty() && ItemStack.canCombine(itemStack, stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(final TagKey<Item> tag) {
        final int size = size();
        for (int i = 0; i < size; i++) {
            if (getStack(i).isIn(tag)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int indexOf(final ItemStack stack) {
        final int count = delegate.main().getSlotCount();
        for (int i = 0; i < count; ++i) {
            final ItemStack itemStack = getStack(i);
            if (!itemStack.isEmpty() && ItemStack.canCombine(stack, itemStack) && !itemStack.isDamaged() && !itemStack.hasEnchantments() && !itemStack.hasCustomName()) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public ItemStack getStack(final int slot) {
        final InventorySlot mapped = map(slot);
        final SingleSlotStorage<ItemVariant> storage = mapped.slot(delegate);
        return storage.getResource().toStack((int) storage.getAmount());
    }

    @Override
    public ItemStack removeStack(final int slot, final int amount) {
        final ItemStack stack = getStack(slot);
        final ItemStack split = stack.split(amount);
        if (setStack0(slot, stack)) {
            return split;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void removeOne(final ItemStack stack) {
    }

    @Override
    public ItemStack removeStack(final int slot) {
        final ItemStack stack = getStack(slot);
        if (setStack0(slot, ItemStack.EMPTY)) {
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setStack(final int slot, final ItemStack stack) {
        setStack0(slot, stack);
    }

    @Override
    public void dropAll() {
        final int size = size();
        for (int i = 0; i < size; i++) {
            final ItemStack stack = getStack(i);
            setStack0(i, ItemStack.EMPTY);
            player.dropItem(stack, true, false);
        }
    }

    @Override
    public ItemStack getArmorStack(final int slot) {
        return getStack(38 + slot);
    }

    @Override
    public float getBlockBreakingSpeed(final BlockState block) {
        return getStack(0).getMiningSpeedMultiplier(block);
    }

    private InventorySlot map(final int index) {
        if (index == 0) {
            return new InventorySlot(EquipmentSlot.MAINHAND);
        }
        if (index < 36) {
            return new InventorySlot(index - 1);
        } else if (index < 40) {
            return new InventorySlot(EquipmentSlot.values()[index - 34]);
        } else if (index == 40) {
            return new InventorySlot(EquipmentSlot.OFFHAND);
        }
        return new InventorySlot(index - 5);
    }
}
