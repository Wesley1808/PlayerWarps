package me.wesley1808.playerwarps.util;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.wesley1808.playerwarps.command.PlayerWarpCommand;
import me.wesley1808.playerwarps.data.PlayerWarp;
import me.wesley1808.playerwarps.data.PlayerWarpManager;
import net.minecraft.commands.CommandSourceStack;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.Collections.singletonList;

public final class CommandSuggestions {

    public static SuggestionProvider<CommandSourceStack> enabledPlayerWarps() {
        return (ctx, builder) -> suggest(builder, Util.map(PlayerWarpManager.getWarps(warp -> !warp.isDisabled()), PlayerWarp::getName));
    }

    public static SuggestionProvider<CommandSourceStack> playerWarpsOwned() {
        return (ctx, builder) -> {
            UUID uuid = ctx.getSource().getPlayerOrException().getUUID();
            boolean isAdmin = Permissions.check(ctx.getSource(), Permission.ADMIN);
            return suggest(builder, Util.map(PlayerWarpManager.getWarps(warp -> isAdmin || warp.getOwner().equals(uuid)), PlayerWarp::getName));
        };
    }

    public static SuggestionProvider<CommandSourceStack> playerWarpDescription() {
        return (ctx, builder) -> {
            try {
                String warpName = StringArgumentType.getString(ctx, PlayerWarpCommand.WARP_NAME_KEY);
                PlayerWarp warp = PlayerWarpManager.getWarpByName(warpName);
                return suggest(builder, warp == null || warp.getDescription() == null ? Collections.emptyList() : singletonList(warp.getDescription()));
            } catch (IllegalArgumentException ignored) {
                return Suggestions.empty();
            }
        };
    }

    public static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, Collection<String> list) {
        if (list.isEmpty()) {
            return Suggestions.empty();
        }

        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String str : list) {
            if (str.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(str);
            }
        }
        return builder.buildFuture();
    }
}
