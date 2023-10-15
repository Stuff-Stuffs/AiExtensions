package io.github.stuff_stuffs.aiex.common.impl;

import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReference;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReferenceData;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReferenceDataType;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class EntityReferenceImpl implements EntityReference {
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

    public static Optional<NbtCompound> encode(final EntityReferenceImpl reference, final boolean encodeUuid) {
        final NbtCompound compound = new NbtCompound();
        if (encodeUuid) {
            compound.putString("uuid", reference.uuid.toString());
        }
        compound.putString("lastSeenOn", reference.lastSeenOn.getValue().toString());
        final Optional<RegistryKey<EntityType<?>>> key = reference.type.getKey();
        if (key.isEmpty()) {
            return Optional.empty();
        }
        compound.putString("type", key.get().getValue().toString());
        final NbtCompound data = new NbtCompound();
        for (final Map.Entry<EntityReferenceDataType<?, ?>, EntityReferenceData> entry : reference.data.entrySet()) {
            final Optional<NbtElement> result = EntityReferenceData.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue()).result();
            if (result.isPresent()) {
                final Identifier id = EntityReferenceDataType.REGISTRY.getId(entry.getKey());
                if (id != null) {
                    data.put(id.toString(), result.get());
                }
            }
        }
        if (!data.isEmpty()) {
            compound.put("data", data);
        }
        return Optional.of(compound);
    }

    public static Optional<EntityReferenceImpl> decode(@Nullable UUID id, final NbtCompound nbt) {
        if (id == null) {
            final String uuidString = nbt.getString("id");
            if (uuidString.isEmpty()) {
                throw new IllegalStateException("Tried to decode with missing uuid!");
            }
            id = UUID.fromString(uuidString);
        }
        final String lastSeenString = nbt.getString("lastSeenOn");
        if (!Identifier.isValid(lastSeenString)) {
            return Optional.empty();
        }
        final RegistryKey<World> lastSeenOn = RegistryKey.of(RegistryKeys.WORLD, new Identifier(lastSeenString));
        final String typeString = nbt.getString("type");
        if (!Identifier.isValid(typeString)) {
            return Optional.empty();
        }
        final Optional<RegistryEntry.Reference<EntityType<?>>> type = Registries.ENTITY_TYPE.getEntry(RegistryKey.of(RegistryKeys.ENTITY_TYPE, new Identifier(typeString)));
        if (type.isEmpty()) {
            return Optional.empty();
        }
        final Map<EntityReferenceDataType<?, ?>, EntityReferenceData> data = new Reference2ObjectOpenHashMap<>();
        if (nbt.contains("data", NbtElement.COMPOUND_TYPE)) {
            final NbtCompound dataMap = nbt.getCompound("data");
            for (final String key : dataMap.getKeys()) {
                if (!Identifier.isValid(key)) {
                    continue;
                }
                final EntityReferenceDataType<?, ?> dataType = EntityReferenceDataType.REGISTRY.get(new Identifier(key));
                if (dataType == null) {
                    continue;
                }
                final Optional<? extends EntityReferenceData> result = dataType.codec().parse(NbtOps.INSTANCE, dataMap.get(key)).result();
                if (result.isPresent()) {
                    data.put(dataType, result.get());
                }
            }
        }
        return Optional.of(new EntityReferenceImpl(id, type.get(), lastSeenOn, data));
    }
}
