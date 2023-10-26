package io.github.stuff_stuffs.aiex.common.api.entity;

import com.google.common.collect.MapMaker;
import com.mojang.authlib.GameProfile;
import io.github.stuff_stuffs.aiex.common.internal.FakePlayerAsm;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class AiFakePlayer extends FakePlayer {
    private static final Map<UUID, GameProfile> GAME_PROFILE_CACHE = new MapMaker().weakValues().concurrencyLevel(1).makeMap();
    protected final Entity delegate;
    public @Nullable WeakReference<Inventory> openInventory;

    public static <T extends Entity> AiFakePlayer create(final T entity, final ServerWorld world) {
        return FakePlayerAsm.get(entity, world);
    }

    protected AiFakePlayer(final ServerWorld world, final Entity delegate) {
        super(world, getCached(delegate));
        this.delegate = delegate;
    }

    @Override
    @NoGenerateDelegate
    public EntityType<?> getType() {
        return super.getType();
    }

    public Entity getDelegate() {
        return delegate;
    }

    private static GameProfile getCached(final Entity entity) {
        GameProfile profile = GAME_PROFILE_CACHE.get(entity.getUuid());
        if (profile == null) {
            profile = new GameProfile(null, entity.getUuid() + "'s delegate");
            GAME_PROFILE_CACHE.put(entity.getUuid(), profile);
        }
        return profile;
    }

    @Override
    public ItemStack getProjectileType(final ItemStack stack) {
        if (!(stack.getItem() instanceof RangedWeaponItem)) {
            return ItemStack.EMPTY;
        } else {
            Predicate<ItemStack> predicate = ((RangedWeaponItem) stack.getItem()).getHeldProjectiles();
            final ItemStack itemStack = RangedWeaponItem.getHeldProjectile(this, predicate);
            if (!itemStack.isEmpty()) {
                return itemStack;
            } else {
                predicate = ((RangedWeaponItem) stack.getItem()).getProjectiles();

                final PlayerInventory inventory = getInventory();
                final int size = inventory.size();
                for (int i = 0; i < size; ++i) {
                    final ItemStack itemStack2 = inventory.getStack(i);
                    if (predicate.test(itemStack2)) {
                        return itemStack2;
                    }
                }

                return ItemStack.EMPTY;
            }
        }
    }

    @Override
    public float getBlockBreakingSpeed(final BlockState block) {
        float f = getInventory().getBlockBreakingSpeed(block);
        if (f > 1.0F) {
            final int i = EnchantmentHelper.getEfficiency(this);
            final ItemStack itemStack = getMainHandStack();
            if (i > 0 && !itemStack.isEmpty()) {
                f += (float) (i * i + 1);
            }
        }

        if (StatusEffectUtil.hasHaste(this)) {
            f *= 1.0F + (float) (StatusEffectUtil.getHasteAmplifier(this) + 1) * 0.2F;
        }

        if (hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            f *= switch (getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };
        }

        if (isSubmergedIn(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(this)) {
            f /= 5.0F;
        }

        if (!isOnGround()) {
            f /= 5.0F;
        }

        return f;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface NoGenerateDelegate {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface EnsureDelegateGeneration {
    }
}
