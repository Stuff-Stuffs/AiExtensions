package io.github.stuff_stuffs.aiex.common.internal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.stuff_stuffs.aiex.common.api.debug.PathDebugInfo;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class AiExCommands {
    public static final Map<UUID, Predicate<Entity>> PATH_WATCHING = new Object2ObjectOpenHashMap<>();
    public static Consumer<PathDebugInfo> CLIENT_PATH_DEBUG_APPLICATOR = info -> {
    };

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("aiexPathDebug").requires(source -> source.hasPermissionLevel(2)).then(CommandManager.argument("players", EntityArgumentType.players()).then(CommandManager.argument("targets", EntityArgumentType.entities()).executes(new Command<ServerCommandSource>() {
            @Override
            public int run(final CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                final Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");
                final Set<UUID> targets = EntityArgumentType.getEntities(context, "targets").stream().map(Entity::getUuid).collect(Collectors.toUnmodifiableSet());
                for (final ServerPlayerEntity player : players) {
                    PATH_WATCHING.put(player.getUuid(), i -> targets.contains(i.getUuid()));
                }
                return 0;
            }
        })))));
    }

    private AiExCommands() {
    }
}
