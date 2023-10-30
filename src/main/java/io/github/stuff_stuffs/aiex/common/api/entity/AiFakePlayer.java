package io.github.stuff_stuffs.aiex.common.api.entity;

import com.mojang.authlib.GameProfile;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReferencable;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReference;
import io.github.stuff_stuffs.aiex.common.internal.FakePlayerAsm;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.ref.WeakReference;

public class AiFakePlayer extends FakePlayer implements EntityReferencable {
    protected final Entity delegate;
    public @Nullable WeakReference<Inventory> openInventory;

    public static <T extends Entity> AiFakePlayer create(final T entity, final ServerWorld world) {
        return FakePlayerAsm.get(entity, world);
    }

    protected AiFakePlayer(final ServerWorld world, final Entity delegate) {
        super(world, createProfile(delegate));
        this.delegate = delegate;
    }

    @Override
    @NoGenerateDelegate
    public EntityReference aiex$getAndUpdateReference() {
        return delegate.aiex$getAndUpdateReference();
    }

    @Override
    @NoGenerateDelegate
    public EntityType<?> getType() {
        return super.getType();
    }

    @NoGenerateDelegate
    public Entity getDelegate() {
        return delegate;
    }

    @Override
    @NoGenerateDelegate
    public boolean shouldSave() {
        return false;
    }

    private static GameProfile createProfile(final Entity entity) {
        return new GameProfile(null, entity.getUuid() + "'s delegate");
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
