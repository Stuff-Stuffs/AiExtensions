package io.github.stuff_stuffs.aiex.common.api;

import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.fabricmc.fabric.api.gamerule.v1.CustomGameRuleCategory;
import net.minecraft.text.Text;

public final class AiExGameRules {
    public static final CustomGameRuleCategory NPC_CATEGORY = new CustomGameRuleCategory(AiExCommon.id("npc"), /*FIXME*/Text.of("NPC"));

    public static void init() {
    }

    private AiExGameRules() {
    }
}
