package io.github.stuff_stuffs.aiex.common.api.brain.task;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.util.InventorySlot;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

public final class BasicTasks {
    public static final class Walk {
        public static final TaskKey<Result, Parameters, BrainResourceRepository> KEY = new TaskKey<>(Result.class, Parameters.class, BrainResourceRepository.class);
        public static final TaskKey<Result, DynamicParameters, BrainResourceRepository> DYNAMIC_KEY = new TaskKey<>(Result.class, DynamicParameters.class, BrainResourceRepository.class);

        public enum Result {
            CONTINUE,
            DONE,
            CANNOT_REACH,
            RESOURCE_ACQUISITION_ERROR
        }

        public interface Parameters {
            Vec3d target();

            double maxError();

            default double urgency() {
                return 0.0;
            }

            default double maxPathLength() {
                return 64.0;
            }

            default boolean partial() {
                return true;
            }
        }

        public interface DynamicParameters extends Parameters {
            boolean shouldStop();
        }

        private Walk() {
        }
    }

    public static final class Attack {
        public static final TaskKey<Result, Parameters, BrainResourceRepository> KEY = new TaskKey<>(Result.class, Parameters.class, BrainResourceRepository.class);

        public sealed interface Result {
        }

        public record ResourceAcquisitionError() implements Result {
        }

        public record EntityNotFoundFailure() implements Result {
        }

        public record EntityTooFarAway() implements Result {
        }

        public record GenericFailure(@Nullable Object context) implements Result {
        }

        public record CooldownWait(float progress) implements Result {
        }

        public record Success(float damageDone, boolean killed) implements Result {
        }

        public interface Parameters {
            UUID target();

            default double urgency() {
                return 0.0;
            }
        }

        private Attack() {
        }
    }

    public static final class Look {
        public static final TaskKey<Result, Parameters, BrainResourceRepository> KEY = new TaskKey<>(Result.class, Parameters.class, BrainResourceRepository.class);
        public static final TaskKey<Result, Parameters, BrainResourceRepository> DYNAMIC_KEY = new TaskKey<>(Result.class, Parameters.class, BrainResourceRepository.class);
        public static final TaskKey<Result, EntityParameters, BrainResourceRepository> ENTITY_KEY = new TaskKey<>(Result.class, EntityParameters.class, BrainResourceRepository.class);
        public static final TaskKey<Result, EntityParameters, BrainResourceRepository> ENTITY_DYNAMIC_KEY = new TaskKey<>(Result.class, EntityParameters.class, BrainResourceRepository.class);

        public enum Result {
            CONTINUE,
            FAILED,
            ALIGNED,
            RESOURCE_ACQUISITION_ERROR
        }

        public interface Parameters {
            Vec3d lookDir();

            double lookSpeed();
        }

        public interface EntityParameters {
            Entity entity();

            RaycastContext.ShapeType type();

            double lookSpeed();
        }

        private Look() {
        }
    }

    public static final class UseItem {
        public static final TaskKey<Result, Parameters, BrainResourceRepository> KEY = new TaskKey<>(Result.class, Parameters.class, BrainResourceRepository.class);

        public sealed interface Result {
        }

        public record ResourceAcquisitionError() implements Result {
        }

        public record UsingOtherHandError() implements Result {
        }

        public record Use(ActionResult result, ItemStack newStack) implements Result {
        }

        public record UseOnBlock(ActionResult result) implements Result {
        }

        public record UseOnEntity(ActionResult result) implements Result {
        }

        public record UseTick(int time, int timeLeft) implements Result {
        }

        public record Finished(ItemStack newStack) implements Result {
        }

        public record Miss() implements Result {
        }

        public record CooldownWait(float progress) implements Result {
        }

        public sealed interface Parameters {
            Hand hand();
        }

        public record BlockParameters(Hand hand, BlockPos pos) implements Parameters {
        }

        public record EntityParameters(Hand hand, LivingEntity entity) implements Parameters {
        }

        public record AutoParameters(Hand hand) implements Parameters {
        }

        private UseItem() {
        }
    }

    public static final class SwapStack {
        public static final TaskKey<Result, Parameters, BrainResourceRepository> KEY = new TaskKey<>(Result.class, Parameters.class, BrainResourceRepository.class);

        public enum Result {
            OK,
            ERROR,
            RESOURCE_ACQUISITION_ERROR
        }

        public interface Parameters {
            InventorySlot source();

            InventorySlot destination();
        }

        private SwapStack() {
        }
    }

    public static final class SelectToolTask {
        public static final TaskKey<Result, Parameters, BrainResourceRepository> KEY = new TaskKey<>(Result.class, Parameters.class, BrainResourceRepository.class);

        public sealed interface Result {
        }

        public record NoneAvailableError() implements Result {
        }

        public record Success(List<InventorySlot> bestToWorst) implements Result {
        }

        public interface Parameters {
            BlockState state();
        }

        private SelectToolTask() {
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static final class MoveItemsToContainerTask {
        public static final TaskKey<Result, Parameters, BrainResourceRepository> KEY = new TaskKey<>(Result.class, Parameters.class, BrainResourceRepository.class);

        public sealed interface Result {
            Object2LongMap<ItemVariant> moved();
        }

        public record Continue(Object2LongMap<ItemVariant> moved) implements Result {
        }

        public record Success(Object2LongMap<ItemVariant> moved) implements Result {
        }

        public record Error(ErrorType type, Object2LongMap<ItemVariant> moved) implements Result {
        }

        public enum ErrorType {
            CANNOT_REACH,
            MISSING_CONTAINER
        }

        public interface Parameters {
            Filter filter();

            BlockPos container();

            @Nullable Direction side();

            default int speed() {
                return 10;
            }
        }

        public interface Filter {
            Amount filter(BrainContext<?> context, ItemVariant variant, long amount, InventorySlot slot, SlottedStorage<ItemVariant> container);
        }

        public record Amount(long amount, OptionalInt target) {
        }

        private MoveItemsToContainerTask() {
        }
    }

    public static void init() {
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("basic_walk"), Walk.KEY);
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("dynamic_walk"), Walk.DYNAMIC_KEY);
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("basic_attack"), Attack.KEY);
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("basic_look"), Look.KEY);
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("dynamic_look"), Look.DYNAMIC_KEY);
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("basic_look_entity"), Look.ENTITY_KEY);
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("dynamic_look_entity"), Look.ENTITY_DYNAMIC_KEY);
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("basic_use_item"), UseItem.KEY);
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("swap_stack"), SwapStack.KEY);
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("select_tool"), SelectToolTask.KEY);
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("move_items_to_container"), MoveItemsToContainerTask.KEY);
    }

    private BasicTasks() {
    }
}
