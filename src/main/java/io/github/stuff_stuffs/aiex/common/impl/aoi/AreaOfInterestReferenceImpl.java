package io.github.stuff_stuffs.aiex.common.impl.aoi;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterest;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestReference;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

public class AreaOfInterestReferenceImpl<T extends AreaOfInterest> implements AreaOfInterestReference<T> {
    public static final Codec<AreaOfInterestReferenceImpl<?>> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("id").forGetter(AreaOfInterestReferenceImpl::id),
            RegistryKey.createCodec(RegistryKeys.WORLD).fieldOf("world").forGetter(AreaOfInterestReferenceImpl::world),
            AreaOfInterestType.REGISTRY.getCodec().fieldOf("type").forGetter(AreaOfInterestReferenceImpl::type)
    ).apply(instance, AreaOfInterestReferenceImpl::new));
    private final long id;
    private final RegistryKey<World> world;
    private final AreaOfInterestType<T> type;

    public AreaOfInterestReferenceImpl(final long id, final RegistryKey<World> world, final AreaOfInterestType<T> type) {
        this.id = id;
        this.world = world;
        this.type = type;
    }

    @Override
    public AreaOfInterestType<T> type() {
        return type;
    }

    public long id() {
        return id;
    }

    public RegistryKey<World> world() {
        return world;
    }
}
