package io.github.stuff_stuffs.aiex.common.api.entity.pathing;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.LocationClassifier;

import java.util.Collection;

public interface PathingNpcEntity {
    default int priorityEnsuredPathingRadius() {
        return 1;
    }

    default int priorityPathingPollRate() {
        return 15;
    }

    default int ensuredPathingRadius() {
        return 3;
    }

    default int pathingPollRate() {
        return 40;
    }

    Collection<LocationClassifier<?>> ensuredLocationClassifiers();
}
