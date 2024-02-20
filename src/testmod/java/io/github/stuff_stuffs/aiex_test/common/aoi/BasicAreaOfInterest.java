package io.github.stuff_stuffs.aiex_test.common.aoi;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.aiex.common.api.AiWorldExtensions;
import io.github.stuff_stuffs.aiex.common.api.aoi.*;
import io.github.stuff_stuffs.aiex_test.common.AiExTestCommon;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

public class BasicAreaOfInterest implements AreaOfInterest, TickingAreaOfInterest {
    public static final Codec<BasicAreaOfInterest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("source").forGetter(BasicAreaOfInterest::source),
            Uuids.STRING_CODEC.optionalFieldOf("claimedBy").forGetter(BasicAreaOfInterest::claimedBy),
            Codec.LONG.fieldOf("lastVisited").forGetter(BasicAreaOfInterest::lastVisited)
    ).apply(instance, BasicAreaOfInterest::new));
    private static final long TIMEOUT = 20 * 45; //45 seconds;
    private final BlockPos source;
    private Optional<UUID> claimedBy;
    private long lastVisited;
    private AreaOfInterestReference<?> ref = null;

    public BasicAreaOfInterest(final BlockPos source, final Optional<UUID> claimedBy, final long lastVisited) {
        this.source = source;
        this.claimedBy = claimedBy;
        this.lastVisited = lastVisited;
    }

    @Override
    public void setRef(final AreaOfInterestReference<?> thisRef) {
        ref = thisRef;
    }

    public BlockPos source() {
        return source;
    }

    public Optional<UUID> claimedBy() {
        return claimedBy;
    }

    public boolean isClaimedBy(final UUID id) {
        if (claimedBy.isPresent()) {
            return claimedBy.get().equals(id);
        }
        return false;
    }

    public long lastVisited() {
        return lastVisited;
    }

    public boolean visit(final ServerWorld world, final Entity entity) {
        if (claimedBy.isEmpty()) {
            lastVisited = world.getTime();
            claimedBy = Optional.of(entity.getUuid());
        } else if (claimedBy.get().equals(entity.getUuid())) {
            lastVisited = world.getTime();
        } else {
            return false;
        }
        if (ref == null) {
            throw new IllegalStateException("Setup not called before visit!");
        }
        entity.aiex$getAndUpdateReference();
        ((AiWorldExtensions) world).aiex$getAoiWorld().markDirty(ref);
        return true;
    }

    @Override
    public void tick(final World world, final AreaOfInterestBounds bounds, final AreaOfInterestReference<?> thisReference) {
        if (lastVisited + TIMEOUT < world.getTime()) {
            claimedBy = Optional.empty();
            ((AiWorldExtensions) world).aiex$getAoiWorld().markDirty(thisReference);
        }
    }
}
