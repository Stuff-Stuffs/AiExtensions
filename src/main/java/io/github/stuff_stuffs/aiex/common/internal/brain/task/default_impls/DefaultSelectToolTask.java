package io.github.stuff_stuffs.aiex.common.internal.brain.task.default_impls;

import io.github.stuff_stuffs.aiex.common.api.AiExApi;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.entity.inventory.NpcInventory;
import io.github.stuff_stuffs.aiex.common.api.util.InventorySlot;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalEnchantmentTags;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class DefaultSelectToolTask<T extends Entity> implements BrainNode<T, BasicTasks.SelectTool.Result, BrainResourceRepository> {
    private final BlockState state;
    private final double increaseBlockDropWeight;

    public DefaultSelectToolTask(final BlockState state, final double increaseBlockDropWeight) {
        this.state = state;
        this.increaseBlockDropWeight = increaseBlockDropWeight;
    }

    @Override
    public void init(final BrainContext<T> context, final SpannedLogger logger) {

    }

    @Override
    public BasicTasks.SelectTool.Result tick(final BrainContext<T> context, final BrainResourceRepository arg, final SpannedLogger logger) {
        final NpcInventory inventory = AiExApi.NPC_INVENTORY.find(context.entity(), null);
        if (inventory == null) {
            throw new IllegalStateException();
        }
        final List<ScoreValuePair> pairs = new ArrayList<>();
        final SlottedStorage<ItemVariant> main = inventory.main();
        final int slotCount = main.getSlotCount();

        for (int i = 0; i < slotCount; i++) {
            final SingleSlotStorage<ItemVariant> slot = main.getSlot(i);
            final ItemVariant resource = slot.getResource();
            if (resource.isBlank()) {
                continue;
            }
            final ItemStack stack = resource.toStack(1);
            final boolean suitable = stack.getItem().isSuitableFor(stack, state);
            if (suitable) {
                final double score = score(stack);
                pairs.add(new ScoreValuePair(score, new InventorySlot(i)));
            }
        }
        {
            final SingleSlotStorage<ItemVariant> mainHandSlot = inventory.equipment().getSlot(EquipmentSlot.MAINHAND.getArmorStandSlotId());
            final ItemStack stack = mainHandSlot.getResource().toStack((int) Math.min(mainHandSlot.getAmount(), Integer.MAX_VALUE));
            final boolean suitable = stack.getItem().isSuitableFor(stack, state);
            if (suitable) {
                pairs.add(new ScoreValuePair(score(stack), new InventorySlot(EquipmentSlot.MAINHAND)));
            }
        }
        {
            final SingleSlotStorage<ItemVariant> mainHandSlot = inventory.equipment().getSlot(EquipmentSlot.OFFHAND.getArmorStandSlotId());
            final ItemStack stack = mainHandSlot.getResource().toStack((int) Math.min(mainHandSlot.getAmount(), Integer.MAX_VALUE));
            final boolean suitable = stack.getItem().isSuitableFor(stack, state);
            if (suitable) {
                pairs.add(new ScoreValuePair(score(stack), new InventorySlot(EquipmentSlot.OFFHAND)));
            }
        }
        if (pairs.isEmpty()) {
            return new BasicTasks.SelectTool.NoneAvailableError();
        }
        pairs.sort(ScoreValuePair.COMPARATOR);
        final List<InventorySlot> slots = new ArrayList<>(pairs.size());
        for (final ScoreValuePair pair : pairs) {
            slots.add(pair.value);
        }
        return new BasicTasks.SelectTool.Success(slots);
    }

    private record ScoreValuePair(double score, InventorySlot value) {
        public static final Comparator<ScoreValuePair> COMPARATOR = Comparator.comparingDouble(ScoreValuePair::score);
    }

    protected double score(final ItemStack stack) {
        double score = stack.getMiningSpeedMultiplier(state);
        if (state.isIn(AiExApi.MINEABLE_ORE_TAG)) {
            final Map<Enchantment, Integer> map = EnchantmentHelper.fromNbt(stack.getEnchantments());
            for (final Map.Entry<Enchantment, Integer> entry : map.entrySet()) {
                score = enchantmentModifyScore(score, entry.getKey(), entry.getValue());
            }
        }
        return score;
    }

    protected double enchantmentModifyScore(double score, final Enchantment enchantment, final int level) {
        final boolean increaseBlockDrop = Registries.ENCHANTMENT.getEntry(enchantment).isIn(ConventionalEnchantmentTags.INCREASES_BLOCK_DROPS);
        if (increaseBlockDrop) {
            score = score + level * increaseBlockDropWeight;
        }
        return score;
    }

    @Override
    public void deinit(final BrainContext<T> context, final SpannedLogger logger) {

    }
}
