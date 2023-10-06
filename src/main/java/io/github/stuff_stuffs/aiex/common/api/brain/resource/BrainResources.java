package io.github.stuff_stuffs.aiex.common.api.brain.resource;

import java.util.Optional;

public interface BrainResources {
    Optional<Token> get(BrainResource resource);

    void release(Token token);

    interface Token {
        BrainResource resource();

        boolean active();
    }
}
