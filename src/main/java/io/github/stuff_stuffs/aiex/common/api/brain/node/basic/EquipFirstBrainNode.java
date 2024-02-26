package io.github.stuff_stuffs.aiex.common.api.brain.node.basic;

import io.github.stuff_stuffs.aiex.common.api.AiExApi;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.entity.inventory.NpcInventory;
import io.github.stuff_stuffs.aiex.common.api.util.InventorySlot;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import net.minecraft.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

public class EquipFirstBrainNode<C extends LivingEntity> implements BrainNode<C, EquipFirstBrainNode.Result, EquipFirstBrainNode.Arguments> {
    private final InventorySlot destination;

    public EquipFirstBrainNode(final InventorySlot destination) {
        this.destination = destination;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {

    }

    @Override
    public Result tick(final BrainContext<C> context, final Arguments arg, final SpannedLogger logger) {
        final NpcInventory inventory = AiExApi.NPC_INVENTORY.find(context.entity(), null);
        if (inventory == null) {
            throw new IllegalStateException();
        }
        final List<InventorySlot> slots = arg.bestToWorst();
        if (slots.isEmpty()) {
            return new Error(ErrorType.NONE_AVAILABLE_ERROR);
        }
        for (final InventorySlot slot : slots) {
            if (slot.equals(destination)) {
                return new Success(slot);
            }
            final Optional<BrainNode<C, BasicTasks.SwapStack.Result, BrainResourceRepository>> task = context.createTask(BasicTasks.SwapStack.KEY, new BasicTasks.SwapStack.Parameters() {
                @Override
                public InventorySlot source() {
                    return slot;
                }

                @Override
                public InventorySlot destination() {
                    return destination;
                }
            }, logger);
            if (task.isEmpty()) {
                return new Error(ErrorType.TASK_SPAWN_ERROR);
            }
            final BrainNode<C, BasicTasks.SwapStack.Result, BrainResourceRepository> node = task.get();
            node.init(context, logger);
            final BasicTasks.SwapStack.Result tick = node.tick(context, arg.repository(), logger);
            node.deinit(context, logger);
            if (tick == BasicTasks.SwapStack.Result.OK) {
                return new Success(slot);
            }
        }
        return new Error(ErrorType.NONE_AVAILABLE_ERROR);
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {

    }

    public interface Arguments {
        List<InventorySlot> bestToWorst();

        BrainResourceRepository repository();
    }

    public sealed interface Result {
    }

    public record Success(InventorySlot slot) implements Result {
    }

    public record Error(ErrorType type) implements Result {
    }

    public enum ErrorType {
        NONE_AVAILABLE_ERROR,
        TASK_SPAWN_ERROR
    }
}
