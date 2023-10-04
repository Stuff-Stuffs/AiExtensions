package io.github.stuff_stuffs.aiex.common.api;

import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.fabricmc.fabric.api.gamerule.v1.CustomGameRuleCategory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.gamerule.v1.rule.EnumRule;
import net.minecraft.text.Text;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;

public final class AiExGameRules {
    public static final CustomGameRuleCategory NPC_CATEGORY = new CustomGameRuleCategory(AiExCommon.id("npc"), /*FIXME*/Text.of("NPC"));
    public static final GameRules.Key<GameRules.BooleanRule> NPC_STARVATION = GameRuleRegistry.register(AiExCommon.MOD_ID + ":NpcStarvation", NPC_CATEGORY, GameRuleFactory.createBooleanRule(false));
    public static final GameRules.Key<EnumRule<Difficulty>> NPC_DIFFICULTY = GameRuleRegistry.register(AiExCommon.MOD_ID + ":NpcDifficulty", NPC_CATEGORY, GameRuleFactory.createEnumRule(Difficulty.NORMAL));

    public static void init() {
        CustomGameRuleCategory.getCategory(NPC_STARVATION);
        CustomGameRuleCategory.getCategory(NPC_DIFFICULTY);
    }

    private AiExGameRules() {
    }
}
