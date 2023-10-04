package io.github.stuff_stuffs.aiex.common.api.entity.inventory;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class BasicNpcInventory implements NpcInventory {
    private final BasicStorage main;
    private final BasicStorage equipment;

    public BasicNpcInventory(final int mainSize) {
        if (mainSize < 2) {
            throw new IllegalArgumentException();
        }
        main = new BasicStorage(mainSize);
        equipment = new BasicStorage(EquipmentSlot.values().length);
    }

    @Override
    public SlottedStorage<ItemVariant> main() {
        return main;
    }

    @Override
    public SlottedStorage<ItemVariant> equipment() {
        return equipment;
    }

    @Override
    public Iterable<ItemStack> armorItems() {
        final EquipmentSlot[] values = EquipmentSlot.values();
        final List<ItemStack> armorItems = new ArrayList<>(values.length - 2);
        for (final EquipmentSlot slot : values) {
            if (slot.isArmorSlot()) {
                armorItems.add(equipment.stacks[slot.getArmorStandSlotId()].copy());
            }
        }
        return armorItems;
    }

    @Override
    public Iterable<ItemStack> handItems() {
        final EquipmentSlot[] slots = new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
        final List<ItemStack> stacks = new ArrayList<>(slots.length);
        for (final EquipmentSlot slot : slots) {
            stacks.add(equipment.stacks[slot.getArmorStandSlotId()].copy());
        }
        return stacks;
    }

    /*FIXME*/
    @Override
    public ItemStack equip(final ItemStack stack, final EquipmentSlot slot) {
        if (Transaction.isOpen()) {
            return equipment.stacks[slot.getArmorStandSlotId()].copy();
        }
        final ItemStack old = equipment.stacks[slot.getArmorStandSlotId()];
        equipment.stacks[slot.getArmorStandSlotId()] = stack;
        return old;
    }

    @Override
    public NbtCompound writeNbt() {
        final NbtCompound nbt = new NbtCompound();
        final NbtList mainList = new NbtList();
        main.writeNbt(mainList);
        nbt.put("main", mainList);
        final NbtList equipmentList = new NbtList();
        equipment.writeNbt(equipmentList);
        nbt.put("equipment", equipmentList);
        return nbt;
    }

    @Override
    public void readNbt(final NbtCompound nbt) {
        main.readNbt(nbt.getList("main", NbtElement.COMPOUND_TYPE));
        equipment.readNbt(nbt.getList("equipment", NbtElement.COMPOUND_TYPE));
    }

    private static final class BasicStorage implements SlottedStorage<ItemVariant> {
        private final ItemStack[] stacks;
        private final List<BasicSingleSlotStorage> storages;
        private final List<StorageView<ItemVariant>> views;

        public BasicStorage(final int size) {
            stacks = new ItemStack[size];
            Arrays.fill(stacks, ItemStack.EMPTY);
            storages = new ArrayList<>(size);
            views = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                final BasicSingleSlotStorage slot = new BasicSingleSlotStorage(stacks, i);
                storages.add(slot);
                views.add(slot);
            }
        }

        @Override
        public int getSlotCount() {
            return stacks.length;
        }

        @Override
        public SingleSlotStorage<ItemVariant> getSlot(final int slot) {
            return storages.get(slot);
        }

        @Override
        public long insert(final ItemVariant resource, final long maxAmount, final TransactionContext transaction) {
            long l = 0;
            for (final BasicSingleSlotStorage storage : storages) {
                if (storage.getResource().equals(resource)) {
                    l = l + storage.insert(resource, maxAmount - l, transaction);
                    if (l == maxAmount) {
                        break;
                    }
                }
            }
            if (l < maxAmount) {
                for (final BasicSingleSlotStorage storage : storages) {
                    l = l + storage.insert(resource, maxAmount - l, transaction);
                    if (l == maxAmount) {
                        break;
                    }
                }
            }
            return l;
        }

        @Override
        public long extract(final ItemVariant resource, final long maxAmount, final TransactionContext transaction) {
            long l = 0;
            for (final BasicSingleSlotStorage storage : storages) {
                if (storage.getResource().equals(resource)) {
                    l = l + storage.extract(resource, maxAmount, transaction);
                }
            }
            return l;
        }

        @Override
        public Iterator<StorageView<ItemVariant>> iterator() {
            return views.iterator();
        }

        private void writeNbt(final NbtList nbt) {
            for (final ItemStack stack : stacks) {
                final NbtCompound compound = new NbtCompound();
                stack.writeNbt(compound);
                nbt.add(compound);
            }
        }

        private void readNbt(final NbtList nbt) {
            int i = 0;
            for (final NbtElement element : nbt) {
                final NbtCompound compound = (NbtCompound) element;
                final ItemStack stack = ItemStack.fromNbt(compound);
                stacks[i++] = stack;
                if (i == stacks.length) {
                    return;
                }
            }
        }
    }

    private static final class BasicSingleSlotStorage extends SingleStackStorage {
        private final ItemStack[] storage;
        private final int index;

        private BasicSingleSlotStorage(final ItemStack[] storage, final int index) {
            this.storage = storage;
            this.index = index;
        }

        @Override
        protected ItemStack getStack() {
            return storage[index];
        }

        @Override
        protected void setStack(final ItemStack stack) {
            storage[index] = stack;
        }
    }
}
