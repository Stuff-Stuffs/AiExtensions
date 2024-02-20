package io.github.stuff_stuffs.aiex.common.impl.aoi;

import io.github.stuff_stuffs.aiex.common.api.aoi.*;

public class AreaOfInterestEntryImpl<T extends AreaOfInterest> implements AreaOfInterestEntry<T> {
    private final T value;
    private final AreaOfInterestBounds bounds;
    private final AreaOfInterestReferenceImpl<T> reference;

    public AreaOfInterestEntryImpl(final T value, final AreaOfInterestBounds bounds, final AreaOfInterestReferenceImpl<T> reference) {
        this.value = value;
        this.bounds = bounds;
        this.reference = reference;
    }

    @Override
    public T value() {
        return value;
    }

    @Override
    public AreaOfInterestType<T> type() {
        return reference.type();
    }

    @Override
    public AreaOfInterestBounds bounds() {
        return bounds;
    }

    @Override
    public AreaOfInterestReference<T> reference() {
        return reference;
    }
}
