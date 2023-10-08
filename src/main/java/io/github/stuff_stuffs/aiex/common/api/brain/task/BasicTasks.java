package io.github.stuff_stuffs.aiex.common.api.brain.task;

import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class BasicTasks {
    public static final class Walk {
        public static final TaskKey<Result, Parameters> KEY = new TaskKey<>(Result.class, Parameters.class);
        public static final TaskKey<Result, DynamicParameters> DYNAMIC_KEY = new TaskKey<>(Result.class, DynamicParameters.class);

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
        }

        public interface DynamicParameters extends Parameters {
            boolean shouldStop();
        }

        private Walk() {
        }
    }

    public static final class Attack {
        public static final TaskKey<Result, Parameters> KEY = new TaskKey<>(Result.class, Parameters.class);

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

        public record CooldownWait(int ticksRemaining) implements Result {
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

    public static void init() {
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("basic_walk"), Walk.KEY);
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("dynamic_walk"), Walk.DYNAMIC_KEY);
        Registry.register(TaskKey.REGISTRY, AiExCommon.id("basic_attack"), Attack.KEY);
    }

    private BasicTasks() {
    }
}
