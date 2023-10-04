package io.github.stuff_stuffs.aiex.common.api.entity_reference;

import com.mojang.serialization.Codec;

public interface EntityReferenceData {
    Codec<EntityReferenceData> CODEC = EntityReferenceDataType.REGISTRY.getCodec().dispatchStable(EntityReferenceData::type, EntityReferenceDataType::codec);

    EntityReferenceDataType<?, ?> type();
}
