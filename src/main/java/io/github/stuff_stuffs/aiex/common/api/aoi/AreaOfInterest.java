package io.github.stuff_stuffs.aiex.common.api.aoi;

public interface AreaOfInterest {
    AreaOfInterestType<?> type();

    void setRef(AreaOfInterestReference<?> thisRef);
}
