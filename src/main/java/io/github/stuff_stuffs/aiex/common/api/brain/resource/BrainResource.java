package io.github.stuff_stuffs.aiex.common.api.brain.resource;

import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;

public record BrainResource(Identifier id, int maxTicketCount) {
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BrainResource resource)) {
            return false;
        }

        return id.equals(resource.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static BrainResource ofEquipmentSlot(final EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return MAIN_HAND_CONTROL;
        } else if (slot == EquipmentSlot.OFFHAND) {
            return OFF_HAND_CONTROL;
        }
        return new BrainResource(AiExCommon.id("equip_slot" + slot.getArmorStandSlotId()), 1);
    }

    public static BrainResource ofInventorySlot(final int index) {
        return new BrainResource(AiExCommon.id("inv_slot" + index), 1);
    }

    public static final BrainResource HEAD_CONTROL = new BrainResource(AiExCommon.id("control_head"), 1);
    public static final BrainResource BODY_CONTROL = new BrainResource(AiExCommon.id("body_head"), 1);
    public static final BrainResource MAIN_HAND_CONTROL = new BrainResource(AiExCommon.id("main_hand_control"), 1);
    public static final BrainResource OFF_HAND_CONTROL = new BrainResource(AiExCommon.id("off_hand_control"), 1);
}
