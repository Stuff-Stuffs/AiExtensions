package io.github.stuff_stuffs.aiex.common.api.brain.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import net.minecraft.entity.Entity;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class ObservedProjectileEntityBrainEvent extends ObservedEntityBrainEvent {
    public static final Codec<ObservedProjectileEntityBrainEvent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Uuids.STRING_CODEC.fieldOf("uuid").forGetter(ObservedEntityBrainEvent::targetUuid),
            Vec3d.CODEC.fieldOf("position").forGetter(ObservedEntityBrainEvent::position),
            Codec.LONG.fieldOf("timestamp").forGetter(ObservedEntityBrainEvent::lifetime),
            Vec3d.CODEC.fieldOf("velocity").forGetter(ObservedProjectileEntityBrainEvent::velocity),
            Codec.BOOL.fieldOf("gravity").forGetter(ObservedProjectileEntityBrainEvent::hasGravity)
    ).apply(instance, ObservedProjectileEntityBrainEvent::new));
    private final Vec3d velocity;
    private final boolean gravity;

    public ObservedProjectileEntityBrainEvent(final UUID uuid, final Vec3d position, final long timestamp, final Vec3d velocity, final boolean gravity) {
        super(uuid, position, timestamp);
        this.velocity = velocity;
        this.gravity = gravity;
    }

    @Override
    public AiBrainEventType<?> type() {
        return AiBrainEventTypes.OBSERVED_PROJECTILE_ENTITY;
    }

    public Vec3d velocity() {
        return velocity;
    }

    public boolean hasGravity() {
        return gravity;
    }

    public static ObservedProjectileEntityBrainEvent create(final AiBrainView brain, final Entity entity) {
        return new ObservedProjectileEntityBrainEvent(entity.getUuid(), entity.getPos(), brain.age(), entity.getPos().subtract(entity.prevX, entity.prevY, entity.prevZ), !entity.hasNoGravity());
    }
}
