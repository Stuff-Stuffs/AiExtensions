package io.github.stuff_stuffs.aiex.common.api.entity.pathing;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.LocationClassifier;

import java.util.Collection;

public interface PathingNpcEntity {
    int ensuredPathingRadius();

    int pathingCachePollRate();

    Collection<LocationClassifier<?>> ensuredLocationClassifiers();
}
