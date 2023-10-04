package io.github.stuff_stuffs.aiex.common.api.entity.inventory;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

@SuppressWarnings("UnstableApiUsage")
public interface NpcInventory {
    SlottedStorage<ItemVariant> main();

    SlottedStorage<ItemVariant> equipment();

    Iterable<ItemStack> armorItems();

    Iterable<ItemStack> handItems();

    ItemStack equip(ItemStack stack, EquipmentSlot slot);

    NbtCompound writeNbt();

    void readNbt(NbtCompound nbt);
}
