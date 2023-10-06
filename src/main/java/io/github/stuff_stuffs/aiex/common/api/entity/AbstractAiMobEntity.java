package io.github.stuff_stuffs.aiex.common.api.entity;

import com.mojang.serialization.Dynamic;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEventTypes;
import io.github.stuff_stuffs.aiex.common.api.brain.event.DamagedBrainEvent;
import io.github.stuff_stuffs.aiex.common.api.brain.event.ObservedEntityBrainEvent;
import io.github.stuff_stuffs.aiex.common.internal.entity.DummyBrain;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractAiMobEntity extends MobEntity implements AiEntity {
    public static final int VISION_POLLING_RATE = 15;

    protected AbstractAiMobEntity(final EntityType<? extends MobEntity> entityType, final World world) {
        super(entityType, world);
    }

    @Override
    protected Brain<?> deserializeBrain(final Dynamic<?> dynamic) {
        return DummyBrain.create();
    }

    protected abstract void deserializeBrain(NbtCompound brainNbt);

    @Override
    protected void applyDamage(final DamageSource source, final float amount) {
        super.applyDamage(source, amount);
        if (getEntityWorld() instanceof ServerWorld) {
            aiex$getBrain().events().remember(DamagedBrainEvent.create(source, amount));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (getEntityWorld() instanceof ServerWorld world) {
            final double range = observableEntityRange();
            final Box box = Box.of(getPos(), range * 2, range * 2, range * 2);
            final List<? extends Entity> list = world.getEntitiesByClass(observableEntityClass(), box, EntityPredicates.EXCEPT_SPECTATOR.and(entity -> entity != this));
            final double radians = Math.cos(Math.toRadians(fieldOfView()) * 0.5F);
            final Vec3d vector = getRotationVector();
            final AiBrainView brainView = aiex$getBrain();
            final long brainAge = brainView.age();
            final long oldest = brainAge - ObservedEntityBrainEvent.LIFETIME;
            final long youngest = brainAge - VISION_POLLING_RATE;
            final AiBrainView.Events brainEvents = brainView.events();
            final List<ObservedEntityBrainEvent> events = brainEvents.query(AiBrainEventTypes.OBSERVED_ENTITY, youngest);
            final Set<UUID> ids = new ObjectOpenHashSet<>(events.size());
            for (final ObservedEntityBrainEvent event : events) {
                ids.add(event.targetUuid());
            }
            for (final Entity entity : list) {
                if (ids.contains(entity.getUuid())) {
                    continue;
                }
                final Vec3d delta = entity.getPos().subtract(getPos()).normalize();
                if (vector.dotProduct(delta) >= radians && canSee(entity)) {
                    brainEvents.streamQuery(AiBrainEventTypes.OBSERVED_ENTITY, oldest).takeWhile(event -> event.timestamp() <= youngest).forEach(brainEvents::forget);
                    brainEvents.remember(ObservedEntityBrainEvent.create(entity, brainAge));
                }
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(final NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.remove("HandItems");
        nbt.remove("ArmorItems");
        nbt.remove("ArmorDropChances");
        nbt.remove("HandDropChances");
        nbt.remove("Brain");
        final NbtCompound brainNbt = new NbtCompound();
        aiex$getBrain().writeNbt(brainNbt);
        nbt.put("aiexBrain", brainNbt);
    }

    @Override
    public void readCustomDataFromNbt(final NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("aiexBrain", NbtElement.COMPOUND_TYPE)) {
            deserializeBrain(nbt.getCompound("aiexBrain"));
        }
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile();
    }

    @Override
    protected boolean shouldSwimInFluids() {
        return super.shouldSwimInFluids();
    }

    @Override
    public void jump() {
        super.jump();
    }

    @Override
    protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return super.getActiveEyeHeight(pose, dimensions);
    }

    @Override
    protected void updatePotionVisibility() {
        super.updatePotionVisibility();
    }

    @Override
    protected float getSoundVolume() {
        return super.getSoundVolume();
    }

    @Override
    protected void attackLivingEntity(LivingEntity target) {
        super.attackLivingEntity(target);
    }

    @Override
    protected void setFlag(int index, boolean value) {
        super.setFlag(index, value);
    }

    @Override
    protected boolean getFlag(int index) {
        return super.getFlag(index);
    }

    protected float fieldOfView() {
        return 110.0F;
    }

    protected Class<? extends Entity> observableEntityClass() {
        return LivingEntity.class;
    }

    protected double observableEntityRange() {
        return 24.0;
    }
}
