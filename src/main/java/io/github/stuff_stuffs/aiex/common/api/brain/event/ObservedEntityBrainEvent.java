package io.github.stuff_stuffs.aiex.common.api.brain.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.aiex.common.api.AiWorldExtensions;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReference;
import net.minecraft.entity.Entity;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ObservedEntityBrainEvent implements AiBrainEvent {
    public static final long LIFETIME = 30 * SECOND;
    public static final Codec<ObservedEntityBrainEvent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Uuids.STRING_CODEC.fieldOf("uuid").forGetter(ObservedEntityBrainEvent::targetUuid),
            Vec3d.CODEC.fieldOf("position").forGetter(ObservedEntityBrainEvent::position),
            Codec.LONG.fieldOf("timestamp").forGetter(ObservedEntityBrainEvent::lifetime)
    ).apply(instance, ObservedEntityBrainEvent::new));
    private final UUID uuid;
    private final Vec3d position;
    private final long timestamp;

    protected ObservedEntityBrainEvent(final UUID uuid, final Vec3d position, final long timestamp) {
        this.uuid = uuid;
        this.position = position;
        this.timestamp = timestamp;
    }

    public Vec3d position() {
        return position;
    }

    public UUID targetUuid() {
        return uuid;
    }

    public @Nullable EntityReference target(final AiWorldExtensions extensions) {
        return extensions.aiex$getEntityReference(uuid);
    }

    @Override
    public long lifetime() {
        return LIFETIME;
    }

    public long timestamp() {
        return timestamp;
    }

    @Override
    public AiBrainEventType<?> type() {
        return AiBrainEventTypes.OBSERVED_ENTITY;
    }

    public static ObservedEntityBrainEvent create(final Entity entity, final long brainAge) {
        return new ObservedEntityBrainEvent(entity.getUuid(), entity.getPos(), brainAge);
    }
}
