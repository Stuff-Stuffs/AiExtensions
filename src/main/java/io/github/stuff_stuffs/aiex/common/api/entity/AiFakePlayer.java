package io.github.stuff_stuffs.aiex.common.api.entity;

import com.google.common.collect.MapMaker;
import com.mojang.authlib.GameProfile;
import io.github.stuff_stuffs.aiex.common.internal.FakePlayerAsm;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.UUID;

public class AiFakePlayer extends FakePlayer {
    private static final Map<UUID, GameProfile> GAME_PROFILE_CACHE = new MapMaker().weakValues().concurrencyLevel(1).makeMap();
    protected final Entity delegate;

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

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface NoGenerateDelegate {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface EnsureDelegateGeneration {
    }
}
