package io.github.stuff_stuffs.aiex.common.api.entity_reference;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

public interface EntityReference {
    UUID uuid();

    RegistryEntry<EntityType<?>> type();

    RegistryKey<World> lastSeenOn();

    <T extends EntityReferenceData> Optional<T> getData(EntityReferenceDataType<T, ?> type);
}
