package me.wesley1808.playerwarps.util;

import net.fabricmc.fabric.api.permission.v1.PermissionContextOwner;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.function.Predicate;

public class PwarpPerms {
    public static final Identifier EDIT_ICON = id("command.edit_icon");
    public static final Identifier EDIT_DESCRIPTION = id("command.edit_description");
    public static final Identifier RELOAD = id("command.reload");
    public static final Identifier ADMIN = id("command.admin");
    public static final Identifier CREATE_LIMIT = id("create_limit");

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("playerwarps", path);
    }

    public static boolean check(PermissionContextOwner owner, Identifier permission, boolean defaultValue) {
        return owner.checkPermission(permission).orElse(defaultValue);
    }

    public static Predicate<CommandSourceStack> require(Identifier permission, PermissionLevel level) {
        return PermissionPredicates.require(permission, level);
    }

    public static int getIntOption(PermissionContextOwner owner, Identifier permission, int defaultValue) {
        return owner.checkPermission(PermissionNode.ofInteger(permission), defaultValue);
    }
}
