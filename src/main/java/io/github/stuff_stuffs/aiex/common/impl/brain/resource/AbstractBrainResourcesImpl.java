package io.github.stuff_stuffs.aiex.common.impl.brain.resource;

import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;

import java.util.Optional;

public abstract class AbstractBrainResourcesImpl implements BrainResources {
    public abstract void clear();

    public abstract Optional<Token> createChild(Token token);
}
