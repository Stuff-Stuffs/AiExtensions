package io.github.stuff_stuffs.aiex.common.api.util;

import com.mojang.datafixers.util.Either;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.entity.inventory.NpcInventory;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.minecraft.entity.EquipmentSlot;

import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public class InventorySlot {
    private final Either<EquipmentSlot, Integer> internal;

    public InventorySlot(final EquipmentSlot slot) {
        internal = Either.left(slot);
    }

    public InventorySlot(final int mainInventorySlot) {
        internal = Either.right(mainInventorySlot);
    }

    public SingleSlotStorage<ItemVariant> slot(final NpcInventory inventory) {
        return internal.map(s -> inventory.equipment().getSlot(s.getArmorStandSlotId()), i -> inventory.main().getSlot(i));
    }

    public BrainResource resource() {
        return internal.map(BrainResource::ofEquipmentSlot, BrainResource::ofInventorySlot);
    }

    public Optional<EquipmentSlot> equipmentSlot() {
        return internal.left();
    }

    public Optional<Integer> mainSlot() {
        return internal.right();
    }
}
