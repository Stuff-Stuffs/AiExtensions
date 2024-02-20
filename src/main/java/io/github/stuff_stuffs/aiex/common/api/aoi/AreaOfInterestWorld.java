package io.github.stuff_stuffs.aiex.common.api.aoi;

import java.util.Optional;
import java.util.stream.Stream;

public interface AreaOfInterestWorld {
    Stream<AreaOfInterestEntry<?>> intersecting(AreaOfInterestBounds bounds);

    <T extends AreaOfInterest> Stream<AreaOfInterestEntry<T>> intersecting(AreaOfInterestBounds bounds, AreaOfInterestType<T> type);

    <T extends AreaOfInterest> Optional<AreaOfInterestEntry<T>> get(AreaOfInterestReference<T> ref);

    <T extends AreaOfInterest> AreaOfInterestEntry<T> add(AreaOfInterestType<T> type, T value, AreaOfInterestBounds bounds);

    boolean updateBounds(AreaOfInterestReference<?> ref, AreaOfInterestBounds newBounds);

    boolean remove(AreaOfInterestReference<?> ref);

    <T extends AreaOfInterest> boolean update(AreaOfInterestReference<T> ref, T value);

    boolean markDirty(AreaOfInterestReference<?> ref);
}
