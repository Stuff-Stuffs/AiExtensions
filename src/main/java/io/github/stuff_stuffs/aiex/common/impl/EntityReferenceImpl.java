package io.github.stuff_stuffs.aiex.common.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReference;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReferenceData;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReferenceDataType;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Uuids;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class EntityReferenceImpl implements EntityReference {
    public static final Codec<EntityReferenceImpl> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Uuids.STRING_CODEC.fieldOf("uuid").forGetter(EntityReferenceImpl::uuid),
                    Registries.ENTITY_TYPE.createEntryCodec().fieldOf("type").forGetter(EntityReferenceImpl::type),
                    RegistryKey.createCodec(RegistryKeys.WORLD).fieldOf("lastSeenOn").forGetter(EntityReferenceImpl::lastSeenOn),
                    Codec.unboundedMap(EntityReferenceDataType.REGISTRY.getCodec(), EntityReferenceData.CODEC).fieldOf("data").forGetter(impl -> impl.data)
            ).apply(instance, EntityReferenceImpl::new)
    );
    private final UUID uuid;
    private final RegistryEntry<EntityType<?>> type;
    private final RegistryKey<World> lastSeenOn;
    private final Map<EntityReferenceDataType<?, ?>, EntityReferenceData> data;

    public EntityReferenceImpl(final UUID uuid, final RegistryEntry<EntityType<?>> type, final RegistryKey<World> lastSeenOn, final Map<EntityReferenceDataType<?, ?>, EntityReferenceData> data) {
        this.uuid = uuid;
        this.type = type;
        this.lastSeenOn = lastSeenOn;
        this.data = data;
    }

    @Override
    public UUID uuid() {
        return uuid;
    }

    @Override
    public RegistryEntry<EntityType<?>> type() {
        return type;
    }

    @Override
    public RegistryKey<World> lastSeenOn() {
        return lastSeenOn;
    }

    @Override
    public <T extends EntityReferenceData> Optional<T> getData(final EntityReferenceDataType<T, ?> type) {
        final EntityReferenceData data = this.data.get(type);
        if (data != null) {
            //noinspection unchecked
            return Optional.of((T) data);
        }
        return Optional.empty();
    }
}
