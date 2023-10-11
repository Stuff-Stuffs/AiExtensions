package io.github.stuff_stuffs.aiex.common.api.entity;

import io.github.stuff_stuffs.aiex.common.api.AiExGameRules;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrain;
import io.github.stuff_stuffs.aiex.common.api.entity.inventory.BasicNpcInventory;
import io.github.stuff_stuffs.aiex.common.api.entity.inventory.NpcInventory;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public abstract class AbstractNpcEntity extends AbstractAiMobEntity {
    protected final NpcInventory inventory;
    protected final ItemCooldownManager cooldownManager;
    protected NpcHungerManager hungerManager;

    protected AbstractNpcEntity(final EntityType<? extends MobEntity> entityType, final World world) {
        super(entityType, world);
        experiencePoints = 0;
        inventory = createInventory();
        cooldownManager = new ItemCooldownManager();
        hungerManager = createHungerManager(world);
    }

    public NpcHungerManager getHungerManager() {
        return hungerManager;
    }

    public ItemCooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public NpcInventory getInventory() {
        return inventory;
    }

    protected NpcInventory createInventory() {
        return new BasicNpcInventory(44);
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return inventory.armorItems();
    }

    @Override
    public Iterable<ItemStack> getHandItems() {
        return inventory.handItems();
    }

    @Override
    public ItemStack getEquippedStack(final EquipmentSlot slot) {
        final SingleSlotStorage<ItemVariant> storage = inventory.equipment().getSlot(slot.getArmorStandSlotId());
        final ItemVariant resource = storage.getResource();
        if (resource.isBlank()) {
            return ItemStack.EMPTY;
        }
        return resource.toStack((int) storage.getAmount());
    }

    @Override
    public void equipStack(final EquipmentSlot slot, final ItemStack stack) {
        onEquipStack(slot, inventory.equip(stack, slot), stack);
    }

    @Override
    protected void dropEquipment(final DamageSource source, final int lootingMultiplier, final boolean allowDrops) {
        //TODO
    }

    @Override
    protected void initEquipment(final Random random, final LocalDifficulty localDifficulty) {
    }

    @Override
    public void tick() {
        super.tick();
        cooldownManager.update();
        if (getEntityWorld() instanceof ServerWorld world && isAlive()) {
            final AiBrain<?> brain = aiex$getBrain();
            brain.tick();
            hungerManager.tick(this);
        }
    }

    @Override
    public ItemStack eatFood(final World world, final ItemStack stack) {
        final FoodComponent component = stack.getItem().getFoodComponent();
        if (component != null) {
            hungerManager.eat(component);
            world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.NEUTRAL, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
        }
        return super.eatFood(world, stack);
    }

    @Nullable
    @Override
    public SoundEvent getHurtSound(final DamageSource source) {
        return super.getHurtSound(source);
    }

    @Override
    public void takeShieldHit(final LivingEntity attacker) {
        super.takeShieldHit(attacker);
    }

    protected NpcHungerManager createHungerManager(final World world) {
        return new BasicNpcHungerManager(world.getGameRules().get(AiExGameRules.NPC_DIFFICULTY).get(), 20);
    }

    public boolean canFoodHeal() {
        return getHealth() > 0.0F && getHealth() < getMaxHealth();
    }

    public void setDifficulty(final Difficulty difficulty) {
        final NbtCompound compound = hungerManager.writeNbt();
        hungerManager = createHungerManager(getEntityWorld());
        hungerManager.readNbt(compound);
    }

    public interface NpcHungerManager {
        void add(int food, float saturationModifier);

        void eat(FoodComponent component);

        void tick(AbstractNpcEntity entity);

        int getFoodLevel();

        boolean isNotFull();

        void addExhaustion(final float exhaustion);

        float getExhaustion();

        float getSaturationLevel();

        NbtCompound writeNbt();

        void readNbt(NbtCompound nbt);
    }

    public static class BasicNpcHungerManager implements NpcHungerManager {
        protected final Difficulty difficulty;
        protected final int maxFoodLevel;
        protected int foodLevel = 20;
        protected float saturationLevel;
        protected float exhaustion;
        protected int foodTickTimer;

        public BasicNpcHungerManager(final Difficulty difficulty, final int maxFoodLevel) {
            this.difficulty = difficulty;
            this.maxFoodLevel = maxFoodLevel;
        }

        @Override
        public void add(final int food, final float saturationModifier) {
            foodLevel = Math.min(food + foodLevel, maxFoodLevel);
            saturationLevel = Math.min(saturationLevel + (float) food * saturationModifier * 2.0F, (float) foodLevel);
        }

        @Override
        public void eat(final FoodComponent component) {
            add(component.getHunger(), component.getSaturationModifier());
        }

        @Override
        public void tick(final AbstractNpcEntity entity) {
            if (exhaustion > 4.0F) {
                exhaustion -= 4.0F;
                if (saturationLevel > 0.0F) {
                    saturationLevel = Math.max(saturationLevel - 1.0F, 0.0F);
                } else if (difficulty != Difficulty.PEACEFUL) {
                    foodLevel = Math.max(foodLevel - 1, 0);
                }
            }

            final boolean canStarve = entity.getWorld().getGameRules().getBoolean(AiExGameRules.NPC_STARVATION);
            if (canStarve && saturationLevel > 0.0F && entity.canFoodHeal() && foodLevel >= 20) {
                ++foodTickTimer;
                if (foodTickTimer >= 10) {
                    final float f = Math.min(saturationLevel, 6.0F);
                    entity.heal(f / 6.0F);
                    addExhaustion(f);
                    foodTickTimer = 0;
                }
            } else if (canStarve && foodLevel >= 18 && entity.canFoodHeal()) {
                ++foodTickTimer;
                if (foodTickTimer >= 80) {
                    entity.heal(1.0F);
                    addExhaustion(6.0F);
                    foodTickTimer = 0;
                }
            } else if (foodLevel <= 0) {
                ++foodTickTimer;
                if (foodTickTimer >= 80) {
                    if (entity.getHealth() > 10.0F || difficulty == Difficulty.HARD || entity.getHealth() > 1.0F && difficulty == Difficulty.NORMAL) {
                        entity.damage(entity.getDamageSources().starve(), 1.0F);
                    }

                    foodTickTimer = 0;
                }
            } else {
                foodTickTimer = 0;
            }
        }

        @Override
        public int getFoodLevel() {
            return foodLevel;
        }

        @Override
        public boolean isNotFull() {
            return foodLevel < maxFoodLevel;
        }

        @Override
        public void addExhaustion(final float exhaustion) {
            this.exhaustion = Math.min(this.exhaustion + exhaustion, 40.0F);
        }

        @Override
        public float getExhaustion() {
            return exhaustion;
        }

        @Override
        public float getSaturationLevel() {
            return saturationLevel;
        }

        @Override
        public NbtCompound writeNbt() {
            final NbtCompound nbt = new NbtCompound();
            nbt.putInt("level", foodLevel);
            nbt.putFloat("saturation", saturationLevel);
            nbt.putFloat("exhaustion", exhaustion);
            nbt.putInt("foodTick", foodTickTimer);
            return nbt;
        }

        @Override
        public void readNbt(final NbtCompound nbt) {
            foodLevel = Math.min(maxFoodLevel, nbt.getInt("level"));
            saturationLevel = nbt.getFloat("saturation");
            exhaustion = nbt.getFloat("exhaustion");
            foodTickTimer = nbt.getInt("foodTick");
        }
    }
}
