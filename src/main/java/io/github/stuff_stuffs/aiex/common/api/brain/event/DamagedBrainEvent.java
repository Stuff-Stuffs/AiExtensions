package io.github.stuff_stuffs.aiex.common.api.brain.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.aiex.common.api.AiWorldExtensions;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReference;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class DamagedBrainEvent implements AiBrainEvent {
    public static final long LIFETIME = 3 * MINUTE;
    public static final Codec<DamagedBrainEvent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("amount").forGetter(DamagedBrainEvent::amount),
                    Vec3d.CODEC.optionalFieldOf("position").forGetter(DamagedBrainEvent::position),
                    Uuids.STRING_CODEC.optionalFieldOf("attacker").forGetter(DamagedBrainEvent::attackerUuid),
                    Uuids.STRING_CODEC.optionalFieldOf("source").forGetter(DamagedBrainEvent::sourceUuid)
            ).apply(instance, DamagedBrainEvent::new)
    );
    private final float amount;
    private final Optional<Vec3d> position;
    private final Optional<UUID> attacker;
    private final Optional<UUID> source;

    private DamagedBrainEvent(final float amount, final Optional<Vec3d> position, final Optional<UUID> attacker, final Optional<UUID> source) {
        this.amount = amount;
        this.position = position;
        this.attacker = attacker;
        this.source = source;
    }

    public float amount() {
        return amount;
    }

    public Optional<Vec3d> position() {
        return position;
    }

    public Optional<UUID> attackerUuid() {
        return attacker;
    }

    public Optional<UUID> sourceUuid() {
        return source;
    }

    public @Nullable EntityReference attacker(final AiWorldExtensions extensions) {
        return attacker.map(extensions::aiex$getEntityReference).orElse(null);
    }

    public @Nullable EntityReference source(final AiWorldExtensions extensions) {
        return source.map(extensions::aiex$getEntityReference).orElse(null);
    }

    @Override
    public long lifetime() {
        return LIFETIME;
    }

    @Override
    public AiBrainEventType<?> type() {
        return AiBrainEventTypes.DAMAGED;
    }

    public static DamagedBrainEvent create(final DamageSource damageSource, final float amount) {
        final Entity attacker = damageSource.getAttacker();
        final Entity source = damageSource.getSource();
        if (attacker != null) {
            attacker.aiex$getAndUpdateReference();
        }
        if (source != null) {
            source.aiex$getAndUpdateReference();
        }
        return new DamagedBrainEvent(amount, Optional.ofNullable(damageSource.getPosition()), Optional.ofNullable(attacker).map(Entity::getUuid), Optional.ofNullable(damageSource.getSource()).map(Entity::getUuid));
    }
}
