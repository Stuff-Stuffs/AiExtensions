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
    public static final long LIFETIME = 2 * MINUTE;
    public static final Codec<ObservedEntityBrainEvent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Uuids.STRING_CODEC.fieldOf("uuid").forGetter(i -> i.uuid),
            Vec3d.CODEC.fieldOf("position").forGetter(ObservedEntityBrainEvent::position)
    ).apply(instance, ObservedEntityBrainEvent::new));
    private final UUID uuid;
    private final Vec3d position;

    public ObservedEntityBrainEvent(final UUID uuid, final Vec3d position) {
        this.uuid = uuid;
        this.position = position;
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

    @Override
    public AiBrainEventType<?> type() {
        return AiBrainEventTypes.OBSERVED_ENTITY;
    }

    public static ObservedEntityBrainEvent create(final Entity entity) {
        return new ObservedEntityBrainEvent(entity.getUuid(), entity.getPos());
    }
}
