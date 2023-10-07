package io.github.stuff_stuffs.aiex.common.api.entity;

import com.google.common.collect.MapMaker;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;

import java.util.Map;
import java.util.UUID;

public class AiFakePlayer extends FakePlayer {
    private static final Map<UUID, GameProfile> GAME_PROFILE_CACHE = new MapMaker().weakValues().concurrencyLevel(1).makeMap();
    public final Entity delegate;

    public AiFakePlayer(final ServerWorld world, final AbstractNpcEntity delegate) {
        super(world, getCached(delegate));
        this.delegate = delegate;
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
