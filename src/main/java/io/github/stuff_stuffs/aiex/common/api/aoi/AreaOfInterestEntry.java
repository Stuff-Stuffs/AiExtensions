package io.github.stuff_stuffs.aiex.common.api.aoi;

public interface AreaOfInterestEntry<T extends AreaOfInterest> {
    T value();

    AreaOfInterestType<T> type();

    AreaOfInterestBounds bounds();

    AreaOfInterestReference<T> reference();
}
