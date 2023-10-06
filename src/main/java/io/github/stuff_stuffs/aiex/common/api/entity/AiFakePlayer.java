package io.github.stuff_stuffs.aiex.common.api.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTracker;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class AiFakePlayer extends FakePlayer {
    private static final Map<UUID, GameProfile> GAME_PROFILE_CACHE = new WeakHashMap<>();
    public final AbstractNpcEntity delegate;

    public AiFakePlayer(final ServerWorld world, final AbstractNpcEntity delegate) {
        super(world, getCached(delegate));
        this.delegate = delegate;
    }

    public float fieldOfView() {
        return delegate.fieldOfView();
    }

    public Class<? extends Entity> observableEntityClass() {
        return delegate.observableEntityClass();
    }

    public double observableEntityRange() {
        return delegate.observableEntityRange();
    }

    //PASSED HERE IS DELEGATES!
    @Override
    public ItemStack getEquippedStack(final EquipmentSlot slot) {
        return delegate.getEquippedStack(slot);
    }

    @Override
    public void playSound(final SoundEvent sound, final float volume, final float pitch) {
        delegate.playSound(sound, volume, pitch);
    }

    @Override
    public void onDeath(final DamageSource damageSource) {
        delegate.onDeath(damageSource);
    }

    @Override
    protected SoundEvent getHurtSound(final DamageSource source) {
        return delegate.getHurtSound(source);
    }

    @Override
    public boolean damage(final DamageSource source, final float amount) {
        return delegate.damage(source, amount);
    }

    @Override
    protected void takeShieldHit(final LivingEntity attacker) {
        delegate.takeShieldHit(attacker);
    }

    @Override
    public boolean canTakeDamage() {
        return delegate.canTakeDamage();
    }

    @Override
    public void damageArmor(final DamageSource source, final float amount) {
        delegate.damageArmor(source, amount);
    }

    @Override
    public void damageHelmet(final DamageSource source, final float amount) {
        delegate.damageHelmet(source, amount);
    }

    @Override
    public void damageShield(final float amount) {
        delegate.damageShield(amount);
    }

    @Override
    protected void applyDamage(final DamageSource source, final float amount) {
        delegate.applyDamage(source, amount);
    }

    @Override
    public ActionResult interact(final PlayerEntity player, final Hand hand) {
        return delegate.interact(player, hand);
    }

    @Override
    public ActionResult interactAt(final PlayerEntity player, final Vec3d hitPos, final Hand hand) {
        return delegate.interactAt(player, hitPos, hand);
    }

    @Override
    protected boolean isImmobile() {
        return delegate.isImmobile();
    }

    @Override
    public boolean shouldSwimInFluids() {
        return delegate.shouldSwimInFluids();
    }

    @Override
    public void remove(final RemovalReason reason) {
        delegate.remove(reason);
    }

    @Override
    public void wakeUp() {
        delegate.wakeUp();
    }

    @Override
    public void jump() {
        delegate.jump();
    }

    @Override
    public void stopRiding() {
        delegate.stopRiding();
    }

    @Override
    public float getActiveEyeHeight(final EntityPose pose, final EntityDimensions dimensions) {
        return delegate.getActiveEyeHeight(pose, dimensions);
    }

    @Override
    public ItemStack eatFood(final World world, final ItemStack stack) {
        return delegate.eatFood(world, stack);
    }

    @Override
    public boolean isPlayer() {
        return false;
    }

    @Override
    public boolean isBaby() {
        return delegate.isBaby();
    }

    @Override
    public Random getRandom() {
        return delegate.getRandom();
    }

    @Nullable
    @Override
    public LivingEntity getAttacker() {
        return delegate.getAttacker();
    }

    @Override
    public LivingEntity getLastAttacker() {
        return delegate.getLastAttacker();
    }

    @Override
    public void setAttacker(@Nullable final LivingEntity attacker) {
        delegate.setAttacker(attacker);
    }

    @Override
    public void setAttacking(@Nullable final PlayerEntity attacking) {
        delegate.setAttacking(attacking);
    }

    @Override
    public boolean hasNoDrag() {
        return delegate.hasNoDrag();
    }

    @Override
    public boolean hasNoGravity() {
        return delegate.hasNoGravity();
    }

    @Override
    protected void updatePotionVisibility() {
        delegate.updatePotionVisibility();
    }

    @Override
    public Vec3d getPos() {
        return delegate.getPos();
    }

    @Override
    public boolean canTarget(final EntityType<?> type) {
        return delegate.canTarget(type);
    }

    @Override
    public boolean clearStatusEffects() {
        return delegate.clearStatusEffects();
    }

    @Override
    public Collection<StatusEffectInstance> getStatusEffects() {
        return delegate.getStatusEffects();
    }

    @Override
    public Map<StatusEffect, StatusEffectInstance> getActiveStatusEffects() {
        return delegate.getActiveStatusEffects();
    }

    @Override
    public boolean hasStatusEffect(final StatusEffect effect) {
        return delegate.hasStatusEffect(effect);
    }

    @Nullable
    @Override
    public StatusEffectInstance getStatusEffect(final StatusEffect effect) {
        return delegate.getStatusEffect(effect);
    }

    @Override
    public boolean addStatusEffect(final StatusEffectInstance effect, @Nullable final Entity source) {
        return delegate.addStatusEffect(effect, source);
    }

    @Override
    public boolean canHaveStatusEffect(final StatusEffectInstance effect) {
        return delegate.canHaveStatusEffect(effect);
    }

    @Override
    public void setStatusEffect(final StatusEffectInstance effect, @Nullable final Entity source) {
        delegate.setStatusEffect(effect, source);
    }

    @Override
    public boolean isUndead() {
        return delegate.isUndead();
    }

    @Override
    public boolean removeStatusEffect(final StatusEffect type) {
        return delegate.removeStatusEffect(type);
    }

    @Override
    public void heal(final float amount) {
        delegate.heal(amount);
    }

    @Override
    public float getHealth() {
        return delegate.getHealth();
    }

    @Override
    public void setHealth(final float health) {
        delegate.setHealth(health);
    }

    @Override
    public boolean blockedByShield(final DamageSource source) {
        return delegate.blockedByShield(source);
    }

    @Override
    public Identifier getLootTable() {
        return delegate.getLootTable();
    }

    @Override
    public long getLootTableSeed() {
        return delegate.getLootTableSeed();
    }

    @Override
    public boolean isClimbing() {
        return delegate.isClimbing();
    }

    @Override
    public DamageTracker getDamageTracker() {
        return delegate.getDamageTracker();
    }

    @Override
    public void swingHand(final Hand hand) {
        delegate.swingHand(hand);
    }

    @Override
    public void onDamaged(final DamageSource damageSource) {
        delegate.onDamaged(damageSource);
    }

    @Override
    public void handleStatus(final byte status) {
        delegate.handleStatus(status);
    }

    @Nullable
    @Override
    public EntityAttributeInstance getAttributeInstance(final EntityAttribute attribute) {
        return delegate.getAttributeInstance(attribute);
    }

    @Override
    public double getAttributeValue(final RegistryEntry<EntityAttribute> attribute) {
        return delegate.getAttributeValue(attribute);
    }

    @Override
    public double getAttributeValue(final EntityAttribute attribute) {
        return delegate.getAttributeValue(attribute);
    }

    @Override
    public double getAttributeBaseValue(final RegistryEntry<EntityAttribute> attribute) {
        return delegate.getAttributeBaseValue(attribute);
    }

    @Override
    public double getAttributeBaseValue(final EntityAttribute attribute) {
        return delegate.getAttributeBaseValue(attribute);
    }

    @Override
    public AttributeContainer getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public ItemStack getMainHandStack() {
        return delegate.getMainHandStack();
    }

    @Override
    public ItemStack getOffHandStack() {
        return delegate.getOffHandStack();
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return delegate.getArmorItems();
    }

    @Override
    public void equipStack(final EquipmentSlot slot, final ItemStack stack) {
        delegate.equipStack(slot, stack);
    }

    @Override
    public void setSprinting(final boolean sprinting) {
        delegate.setSprinting(sprinting);
    }

    @Override
    protected float getSoundVolume() {
        return delegate.getSoundVolume();
    }

    @Override
    public float getSoundPitch() {
        return delegate.getSoundPitch();
    }

    @Override
    public void pushAwayFrom(final Entity entity) {
        delegate.pushAwayFrom(entity);
    }

    @Override
    public float getMovementSpeed() {
        return delegate.getMovementSpeed();
    }

    @Override
    public void setMovementSpeed(final float movementSpeed) {
        delegate.setMovementSpeed(movementSpeed);
    }

    @Override
    public boolean tryAttack(final Entity target) {
        return delegate.tryAttack(target);
    }

    @Override
    public boolean hurtByWater() {
        return delegate.hurtByWater();
    }

    @Override
    protected void attackLivingEntity(final LivingEntity target) {
        delegate.attackLivingEntity(target);
    }

    @Override
    public boolean isUsingItem() {
        return delegate.isUsingItem();
    }

    @Override
    public boolean isUsingRiptide() {
        return delegate.isUsingRiptide();
    }

    @Override
    public boolean canSee(final Entity entity) {
        return delegate.canSee(entity);
    }

    @Override
    public Arm getMainArm() {
        return delegate.getMainArm();
    }

    @Override
    public Hand getActiveHand() {
        return delegate.getActiveHand();
    }

    @Override
    public void setCurrentHand(final Hand hand) {
        delegate.setCurrentHand(hand);
    }

    @Override
    public void lookAt(final EntityAnchorArgumentType.EntityAnchor anchorPoint, final Vec3d target) {
        delegate.lookAt(anchorPoint, target);
    }

    @Override
    public ItemStack getActiveItem() {
        return delegate.getActiveItem();
    }

    @Override
    public int getItemUseTime() {
        return delegate.getItemUseTime();
    }

    @Override
    public int getItemUseTimeLeft() {
        return delegate.getItemUseTimeLeft();
    }

    @Override
    public void stopUsingItem() {
        delegate.stopUsingItem();
    }

    @Override
    public boolean isBlocking() {
        return delegate.isBlocking();
    }

    @Override
    public boolean canEquip(final ItemStack stack) {
        return delegate.canEquip(stack);
    }

    @Override
    public EntityDimensions getDimensions(final EntityPose pose) {
        return delegate.getDimensions(pose);
    }

    @Override
    public ImmutableList<EntityPose> getPoses() {
        return delegate.getPoses();
    }

    @Override
    public Box getBoundingBox(final EntityPose pose) {
        return delegate.getBoundingBox(pose);
    }

    @Override
    public boolean isGlowing() {
        return delegate.isGlowing();
    }

    @Override
    public boolean disablesShield() {
        return delegate.disablesShield();
    }

    @Override
    public void addVelocity(final Vec3d velocity) {
        delegate.addVelocity(velocity);
    }

    @Override
    public void addVelocity(final double deltaX, final double deltaY, final double deltaZ) {
        delegate.addVelocity(deltaX, deltaY, deltaZ);
    }

    @Override
    public float getPitch() {
        return delegate.getPitch();
    }

    @Override
    public float getYaw() {
        return delegate.getYaw();
    }

    @Override
    public float getPitch(final float tickDelta) {
        return delegate.getPitch(tickDelta);
    }

    @Override
    public float getYaw(final float tickDelta) {
        return delegate.getYaw(tickDelta);
    }

    @Override
    public boolean canBeHitByProjectile() {
        return delegate.canBeHitByProjectile();
    }

    @Override
    public boolean isPushable() {
        return delegate.isPushable();
    }

    @Override
    public boolean isWet() {
        return delegate.isWet();
    }

    @Override
    public boolean isTouchingWater() {
        return delegate.isTouchingWater();
    }

    @Override
    public boolean isInsideWaterOrBubbleColumn() {
        return delegate.isInsideWaterOrBubbleColumn();
    }

    @Override
    public BlockState getSteppingBlockState() {
        return delegate.getSteppingBlockState();
    }

    @Override
    public BlockPos getSteppingPos() {
        return delegate.getSteppingPos();
    }

    @Override
    public World getEntityWorld() {
        return delegate.getEntityWorld();
    }

    @Override
    public World getWorld() {
        return delegate.getWorld();
    }

    @Override
    public boolean isSubmergedIn(final TagKey<Fluid> fluidTag) {
        return delegate.isSubmergedIn(fluidTag);
    }

    @Override
    public boolean isInLava() {
        return delegate.isInLava();
    }

    @Override
    public boolean isSubmergedInWater() {
        return delegate.isSubmergedInWater();
    }

    @Override
    public float distanceTo(final Entity entity) {
        return delegate.distanceTo(entity);
    }

    @Override
    public double squaredDistanceTo(final double x, final double y, final double z) {
        return delegate.squaredDistanceTo(x, y, z);
    }

    @Override
    public double squaredDistanceTo(final Vec3d vector) {
        return delegate.squaredDistanceTo(vector);
    }

    @Override
    public void onPlayerCollision(final PlayerEntity player) {
        delegate.onPlayerCollision(player);
    }

    @Override
    public boolean hasVehicle() {
        return delegate.hasVehicle();
    }

    @Override
    public boolean hasPassengers() {
        return delegate.hasPassengers();
    }

    @Override
    protected void setFlag(final int index, final boolean value) {
        delegate.setFlag(index, value);
    }

    @Override
    protected boolean getFlag(final int index) {
        return delegate.getFlag(index);
    }

    @Override
    public int getMaxAir() {
        return delegate.getMaxAir();
    }

    @Override
    public DataTracker getDataTracker() {
        return delegate.getDataTracker();
    }

    @Override
    public boolean onKilledOther(final ServerWorld world, final LivingEntity other) {
        return delegate.onKilledOther(world, other);
    }

    private static GameProfile getCached(final Entity entity) {
        GameProfile profile = GAME_PROFILE_CACHE.get(entity.getUuid());
        if (profile == null) {
            profile = new GameProfile(null, entity.getUuid() + "'s delegate");
            GAME_PROFILE_CACHE.put(entity.getUuid(), profile);
        }
        return profile;
    }
}
