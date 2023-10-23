package io.github.stuff_stuffs.aiex.common.api.entity.movement;

import java.util.List;

public interface NpcMoveControl {
    void set(List<NpcMovementNode> nodes);

    boolean failedLastPath();

    void tick();

    boolean idle();
}
