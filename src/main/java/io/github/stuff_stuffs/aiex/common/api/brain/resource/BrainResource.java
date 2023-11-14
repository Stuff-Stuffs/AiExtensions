package io.github.stuff_stuffs.aiex.common.api.brain.resource;

import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;

public record BrainResource(Identifier id, Priority priority) {
    public BrainResource withPriority(final Priority priority) {
        if (priority == this.priority) {
            return this;
        }
        return new BrainResource(id, priority);
    }

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
        return ofEquipmentSlot(slot, Priority.ACTIVE);
    }

    public static BrainResource ofEquipmentSlot(final EquipmentSlot slot, final Priority priority) {
        if (slot == EquipmentSlot.MAINHAND) {
            return switch (priority) {
                case ACTIVE -> ACTIVE_MAIN_HAND_CONTROL;
                case PASSIVE -> PASSIVE_MAIN_HAND_CONTROL;
            };
        } else if (slot == EquipmentSlot.OFFHAND) {
            return switch (priority) {
                case ACTIVE -> ACTIVE_OFF_HAND_CONTROL;
                case PASSIVE -> PASSIVE_OFF_HAND_CONTROL;
            };
        }
        return new BrainResource(AiExCommon.id("equip_slot" + slot.getArmorStandSlotId()), priority);
    }

    public static BrainResource ofInventorySlot(final int index) {
        return ofInventorySlot(index, Priority.ACTIVE);
    }

    public static BrainResource ofInventorySlot(final int index, final Priority priority) {
        return new BrainResource(AiExCommon.id("inv_slot" + index), priority);
    }

    public static final BrainResource ACTIVE_HEAD_CONTROL = new BrainResource(AiExCommon.id("control_head"), Priority.ACTIVE);
    public static final BrainResource ACTIVE_BODY_CONTROL = new BrainResource(AiExCommon.id("body_head"), Priority.ACTIVE);
    public static final BrainResource ACTIVE_MAIN_HAND_CONTROL = new BrainResource(AiExCommon.id("main_hand_control"), Priority.ACTIVE);
    public static final BrainResource ACTIVE_OFF_HAND_CONTROL = new BrainResource(AiExCommon.id("off_hand_control"), Priority.ACTIVE);

    public static final BrainResource PASSIVE_HEAD_CONTROL = new BrainResource(AiExCommon.id("control_head"), Priority.PASSIVE);
    public static final BrainResource PASSIVE_BODY_CONTROL = new BrainResource(AiExCommon.id("body_head"), Priority.PASSIVE);
    public static final BrainResource PASSIVE_MAIN_HAND_CONTROL = new BrainResource(AiExCommon.id("main_hand_control"), Priority.PASSIVE);
    public static final BrainResource PASSIVE_OFF_HAND_CONTROL = new BrainResource(AiExCommon.id("off_hand_control"), Priority.PASSIVE);

    public enum Priority {
        PASSIVE,
        ACTIVE
    }
}
