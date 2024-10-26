package me.wesley1808.playerwarps.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.lucko.fabric.api.permissions.v0.Options;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.wesley1808.playerwarps.config.Config;
import me.wesley1808.playerwarps.config.ConfigManager;
import me.wesley1808.playerwarps.data.Location;
import me.wesley1808.playerwarps.data.PlayerWarp;
import me.wesley1808.playerwarps.data.PlayerWarpManager;
import me.wesley1808.playerwarps.gui.GuiHelper;
import me.wesley1808.playerwarps.gui.SimplePagedGui;
import me.wesley1808.playerwarps.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.ResourceArgument.getResource;
import static net.minecraft.commands.arguments.ResourceArgument.resource;

public final class PlayerWarpCommand {
    public static final String WARP_NAME_KEY = "name";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        var builder = literal("pwarp");
        builder.executes(ctx -> openWarpGui(ctx.getSource().getPlayerOrException()));

        builder.then(literal("reload")
                .requires(Permissions.require(Permission.RELOAD, 2))
                .executes(ctx -> reloadConfig(ctx.getSource()))
        );

        builder.then(literal("list")
                .executes(ctx -> openWarpGui(ctx.getSource().getPlayerOrException()))
        );

        builder.then(literal("visit")
                .then(argument(WARP_NAME_KEY, word())
                        .suggests(CommandSuggestions.enabledPlayerWarps())
                        .executes(ctx -> visitWarp(
                                ctx.getSource().getPlayerOrException(),
                                getString(ctx, WARP_NAME_KEY)
                        ))
                )
        );

        builder.then(literal("create")
                .then(argument(WARP_NAME_KEY, word())
                        .executes(ctx -> createWarp(
                                ctx.getSource().getPlayerOrException(),
                                getString(ctx, WARP_NAME_KEY)
                        ))
                )
        );

        builder.then(literal("delete")
                .then(argument(WARP_NAME_KEY, word())
                        .suggests(CommandSuggestions.playerWarpsOwned())
                        .executes(ctx -> deleteWarp(
                                ctx.getSource(),
                                getString(ctx, WARP_NAME_KEY)
                        ))
                )
        );

        builder.then(literal("edit")
                .then(argument(WARP_NAME_KEY, word())
                        .suggests(CommandSuggestions.playerWarpsOwned())
                        .then(literal("description")
                                .requires(Permissions.require(Permission.EDIT_DESCRIPTION, 2))
                                .executes(ctx -> editWarpDescription(
                                        ctx.getSource(),
                                        getString(ctx, WARP_NAME_KEY),
                                        null
                                ))
                                .then(argument("description", greedyString())
                                        .suggests(CommandSuggestions.playerWarpDescription())
                                        .executes(ctx -> editWarpDescription(
                                                ctx.getSource(),
                                                getString(ctx, WARP_NAME_KEY),
                                                getString(ctx, "description")
                                        ))
                                )
                        )
                        .then(literal("icon")
                                .requires(Permissions.require(Permission.EDIT_ICON, 2))
                                .then(argument("icon", resource(buildContext, Registries.ITEM))
                                        .executes(ctx -> editWarpIcon(
                                                ctx.getSource(),
                                                getString(ctx, WARP_NAME_KEY),
                                                getResource(ctx, "icon", Registries.ITEM)
                                        ))
                                )
                        )
                        .then(literal("enable")
                                .executes(ctx -> toggleWarp(
                                        ctx.getSource(),
                                        getString(ctx, WARP_NAME_KEY),
                                        false
                                ))
                        )
                        .then(literal("disable")
                                .executes(ctx -> toggleWarp(
                                        ctx.getSource(),
                                        getString(ctx, WARP_NAME_KEY),
                                        true
                                ))
                        )
                )
        );

        var node = dispatcher.register(builder);
        dispatcher.register(literal("playerwarp").redirect(node));
    }

    private static int reloadConfig(CommandSourceStack source) {
        ConfigManager.load();
        source.sendSuccess(() -> Component.literal("Config reloaded!").withStyle(ChatFormatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int toggleWarp(CommandSourceStack source, String warpName, boolean disabled) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerWarp warp = PlayerWarpManager.getOwnedWarpOrException(source, player, warpName);
        warp.setDisabled(disabled);

        String message = disabled ? Config.instance().messages.warpDisabled : Config.instance().messages.warpEnabled;
        source.sendSuccess(() -> Formatter.parse(message.replace("${name}", warpName)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int editWarpDescription(CommandSourceStack source, String warpName, @Nullable String description) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerWarp warp = PlayerWarpManager.getOwnedWarpOrException(source, player, warpName);
        if (description == null) {
            warp.setDescription(null);
            source.sendSuccess(() -> Formatter.parse(Config.instance().messages.removeWarpDescription), false);
        } else {
            player.getTextFilter().processStreamMessage(description).thenAccept((filteredText) -> {
                String filtered = filteredText.filteredOrEmpty();
                warp.setDescription(filtered);
                source.sendSuccess(() -> Formatter.parse(Config.instance().messages.setWarpDescription.replace("${description}", filtered)), false);
            });
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int editWarpIcon(CommandSourceStack source, String warpName, Holder<Item> icon) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerWarp warp = PlayerWarpManager.getOwnedWarpOrException(source, player, warpName);
        warp.setIcon(icon.value());
        source.sendSuccess(() -> Formatter.parse(Config.instance().messages.setWarpIcon.replace("${item}", BuiltInRegistries.ITEM.getKey(icon.value()).toString())), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int createWarp(ServerPlayer player, String name) {
        List<PlayerWarp> warps = PlayerWarpManager.getWarps(warp -> warp.getOwner().equals(player.getUUID()));
        int maxWarps = Options.get(player, "playerwarps_max", 0, Integer::parseInt);
        if (warps.size() >= maxWarps && !player.hasPermissions(2)) {
            if (maxWarps == 0) {
                player.sendSystemMessage(Formatter.parse(Config.instance().messages.noPermissionToCreate));
            } else {
                player.sendSystemMessage(Formatter.parse(Config.instance().messages.noPermissionToCreateMore.replace("${max}", String.valueOf(maxWarps))));
            }
            return 0;
        }

        String error = Util.isNameAllowed(player, name);
        if (error != null) {
            player.sendSystemMessage(Formatter.parse(error));
            return 0;
        }

        if (!Config.instance().pwarpAllowedWorlds.contains(player.level().dimension().location())) {
            player.sendSystemMessage(Formatter.parse(Config.instance().messages.worldNotAllowed));
            return 0;
        }

        BlockPos pos = player.blockPosition();
        BlockState state = player.level().getBlockState(pos);
        BlockPos warpPos = state.getBlock().hasCollision ? pos.above() : pos;
        if (PlayerWarpManager.isUnsafe(player.serverLevel(), warpPos)) {
            player.sendSystemMessage(Formatter.parse(Config.instance().messages.createUnsafe));
            return 0;
        }

        if (!PlayerWarpManager.add(player.getUUID(), name, player.level().dimension().location(), warpPos)) {
            player.sendSystemMessage(Formatter.parse(Config.instance().messages.nameTaken.replace("${name}", name)));
            return 0;
        }

        player.sendSystemMessage(Formatter.parse(Config.instance().messages.warpCreated.replace("${name}", name)));
        return Command.SINGLE_SUCCESS;
    }

    private static int deleteWarp(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerWarp warp = PlayerWarpManager.getOwnedWarpOrException(source, player, name);

        PlayerWarpManager.remove(warp.getName());
        source.sendSuccess(() -> Formatter.parse(Config.instance().messages.warpDeleted.replace("${name}", name)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int visitWarp(ServerPlayer player, String name) throws CommandSyntaxException {
        PlayerWarp warp = PlayerWarpManager.getWarpOrException(name);
        if (warp.isDisabled()) {
            player.sendSystemMessage(Formatter.parse(Config.instance().messages.disabled));
            return 0;
        }

        if (Scheduler.canSchedule(player.getUUID())) {
            warp.visit(player);
            warp.teleport(player, true, new Location.TeleportPredicate(
                    (level) -> PlayerWarpManager.isUnsafe(level, warp.blockPos),
                    () -> player.sendSystemMessage(Formatter.parse(Config.instance().messages.unsafeTeleport))
            ));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int openWarpGui(ServerPlayer player) {
        SimplePagedGui gui = new SimplePagedGui("Player Warps", player);
        gui.setElements(initializeElements(player, gui));
        return gui.open() ? Command.SINGLE_SUCCESS : 0;
    }

    private static ObjectArrayList<GuiElementInterface> initializeElements(ServerPlayer player, SimpleGui gui) {
        ObjectArrayList<GuiElementInterface> elements = new ObjectArrayList<>();
        ObjectArrayList<PlayerWarp> warps = new ObjectArrayList<>(PlayerWarpManager.getWarps().values());
        Collections.sort(warps);

        for (int i = 0; i < warps.size(); i++) {
            PlayerWarp warp = warps.get(i);
            Item icon = warp.getIcon();
            if (icon == null) {
                icon = i % 2 == 0 ? Items.ENDER_PEARL : Items.ENDER_EYE;
            }

            GuiElementBuilder builder = new GuiElementBuilder(icon);
            builder.setName(warp.isDisabled()
                    ? Component.literal("[Disabled] " + warp.getName()).withStyle(ChatFormatting.RED)
                    : Component.literal(warp.getName()).withStyle(ChatFormatting.GRAY)
            );

            builder.hideDefaultTooltip();
            builder.setLore(getLore(player.server, warp));

            builder.setCallback((index, type, clickType) -> {
                if (type.isLeft || type.isRight) {
                    if (Scheduler.canSchedule(player.getUUID()) && !warp.isDisabled()) {
                        GuiHelper.playSound(player, SoundEvents.UI_BUTTON_CLICK);
                        warp.visit(player);
                        warp.teleport(player, true, new Location.TeleportPredicate(
                                (level) -> PlayerWarpManager.isUnsafe(level, warp.blockPos),
                                () -> player.sendSystemMessage(Formatter.parse(Config.instance().messages.unsafeTeleport))
                        ));
                        gui.close();
                    } else {
                        GuiHelper.playSound(player, Holder.direct(SoundEvents.VILLAGER_NO));
                    }
                }
            });

            elements.add(builder.build());
        }

        return elements;
    }

    private static List<Component> getLore(MinecraftServer server, PlayerWarp warp) {
        List<Component> lore = new ObjectArrayList<>();
        lore.add(Component.literal("Owner: ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .withStyle(style -> style.withItalic(false))
                .append(Component.literal(warp.getOwnerName(server))
                        .withStyle(ChatFormatting.GOLD)
                        .withStyle(style -> style.withItalic(false))
                )
        );

        lore.add(Component.empty());
        lore.add(Component.literal("Visits: ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .withStyle(style -> style.withItalic(false))
                .append(Component.literal(String.valueOf(warp.getVisits()))
                        .withStyle(ChatFormatting.GOLD)
                        .withStyle(style -> style.withItalic(false))
                )
        );

        lore.add(Component.empty());
        lore.add(Component.literal("Coords: ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .withStyle(style -> style.withItalic(false))
                .append(Component.literal(warp.blockPos.toShortString())
                        .withStyle(ChatFormatting.GOLD)
                        .withStyle(style -> style.withItalic(false))
                )
        );

        lore.add(Component.empty());

        String description = warp.getDescription();
        if (description != null && !description.isEmpty()) {
            lore.add(Component.literal("Description:")
                    .withStyle(style -> style.withItalic(false))
                    .withStyle(ChatFormatting.DARK_GRAY)
            );

            String wrapped = WordUtils.wrap(description, 50, System.lineSeparator(), true);
            for (String line : wrapped.split(System.lineSeparator())) {
                lore.add(Component.literal(line)
                        .withStyle(style -> style.withItalic(false))
                        .withStyle(ChatFormatting.GRAY)
                );
            }

            lore.add(Component.empty());
        }

        return lore;
    }
}
