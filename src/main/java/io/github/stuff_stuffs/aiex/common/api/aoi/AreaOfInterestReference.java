package io.github.stuff_stuffs.aiex.common.api.aoi;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.stuff_stuffs.aiex.common.impl.aoi.AreaOfInterestReferenceImpl;

import java.util.function.Function;

public interface AreaOfInterestReference<T extends AreaOfInterest> {
    Codec<AreaOfInterestReference<?>> CODEC = AreaOfInterestReferenceImpl.CODEC.flatComapMap(Function.identity(), reference -> reference instanceof AreaOfInterestReferenceImpl<?> casted ? DataResult.success(casted) : DataResult.error(() -> "Somebody implemented an internal class! AreaOfInterestReference"));

    AreaOfInterestType<T> type();

    static <T extends AreaOfInterest> Codec<AreaOfInterestReference<T>> typeSpecificCodec(final AreaOfInterestType<T> type) {
        //noinspection unchecked
        return CODEC.comapFlatMap(reference -> reference.type() == type ? DataResult.success((AreaOfInterestReference<T>) reference) : DataResult.error(() -> "Type mismatch"), Function.identity());
    }
}
