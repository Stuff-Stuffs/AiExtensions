package io.github.stuff_stuffs.aiex.common.internal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.stuff_stuffs.aiex.common.api.AiWorldExtensions;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestType;
import io.github.stuff_stuffs.aiex.common.api.debug.AiExDebugFlags;
import io.github.stuff_stuffs.aiex.common.internal.debug.AreaOfInterestDebugMessage;
import io.github.stuff_stuffs.aiex.common.internal.debug.PathDebugInfo;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.RegistryEntryArgumentType;
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
    public static final Map<UUID, AreaOfInterestType<?>> AOI_WATCHING = new Object2ObjectOpenHashMap<>();
    public static Consumer<AreaOfInterestDebugMessage> CLIENT_AOI_DEBUG_APPLICATOR = message -> {
    };

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("aiexPathDebug").requires(source -> source.hasPermissionLevel(2)).then(CommandManager.argument("players", EntityArgumentType.players()).then(CommandManager.argument("targets", EntityArgumentType.entities()).executes(context -> {
                final Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");
                final Set<UUID> targets = EntityArgumentType.getEntities(context, "targets").stream().map(Entity::getUuid).collect(Collectors.toUnmodifiableSet());
                for (final ServerPlayerEntity player : players) {
                    PATH_WATCHING.put(player.getUuid(), i -> targets.contains(i.getUuid()));
                }
                return 0;
            }))));
            dispatcher.register(CommandManager.literal("aiexAoiDebug").requires(source -> source.hasPermissionLevel(2)).then(CommandManager.argument("players", EntityArgumentType.players()).executes(new Command<ServerCommandSource>() {
                @Override
                public int run(final CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                    final Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");
                    final Set<AreaOfInterestType<?>> types = new ObjectOpenHashSet<>();
                    for (final ServerPlayerEntity player : players) {
                        final AreaOfInterestType<?> type = AOI_WATCHING.remove(player.getUuid());
                        if (type != null) {
                            types.add(type);
                        }
                    }
                    for (final AreaOfInterestType<?> type : types) {
                        AiExDebugFlags.send(AreaOfInterestDebugMessage.FLAG, new AreaOfInterestDebugMessage.Clear(type), context.getSource().getWorld());
                    }
                    return 0;
                }
            }).then(CommandManager.argument("type", RegistryEntryArgumentType.registryEntry(registryAccess, AreaOfInterestType.REGISTRY_KEY)).executes(context -> {
                final Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");
                final AreaOfInterestType<?> type = RegistryEntryArgumentType.getRegistryEntry(context, "type", AreaOfInterestType.REGISTRY_KEY).value();
                final Set<AreaOfInterestType<?>> oldTypes = new ObjectOpenHashSet<>();
                oldTypes.add(type);
                for (final ServerPlayerEntity player : players) {
                    final AreaOfInterestType<?> old = AOI_WATCHING.put(player.getUuid(), type);
                    if (old != null) {
                        oldTypes.add(old);
                    }
                }
                for (final AreaOfInterestType<?> oldType : oldTypes) {
                    AiExDebugFlags.send(AreaOfInterestDebugMessage.FLAG, new AreaOfInterestDebugMessage.Clear(oldType), context.getSource().getWorld());
                    ((AiWorldExtensions) context.getSource().getWorld()).aiex$resyncAreaOfInterest(oldType);
                }
                return 0;
            }))));
        });
    }

    private AiExCommands() {
    }
}
