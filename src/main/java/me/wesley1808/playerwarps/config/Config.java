package me.wesley1808.playerwarps.config;

import com.google.common.collect.Sets;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public class Config {
    protected static Config instance = new Config();

    public int warpNameMinLength = 3;
    public int warpNameMaxLength = 20;
    public int warpMoveCooldownSeconds = 86400;
    public boolean useStrictTeleportCheck = false;

    public Set<ResourceLocation> pwarpAllowedWorlds = Sets.newHashSet(
            ResourceLocation.withDefaultNamespace("overworld"),
            ResourceLocation.withDefaultNamespace("the_nether"),
            ResourceLocation.withDefaultNamespace("the_end")
    );

    public Messages messages = new Messages();

    public static Config instance() {
        return instance;
    }

    public static class Messages {
        public String tpSecondsLeft = "<yellow>Teleporting in ${seconds} seconds...";
        public String tpCancelled = "<red>[✖] Teleportation was cancelled.";

        public String warpCreated = "<dark_aqua>Warp <green>${name}</green> has been created.";
        public String warpDeleted = "<dark_aqua>Warp <green>${name}</green> has been deleted.";
        public String warpMoved = "<dark_aqua>Warp <green>${name}</green> has been moved to your current position.";
        public String setWarpIcon = "<dark_aqua>Warp icon has been set to <green>${item}";
        public String setWarpDescription = "<dark_aqua>Warp description has been changed:\n<gray>${description}";
        public String removeWarpDescription = "<dark_aqua>Warp description has been removed.";
        public String warpEnabled = "<dark_aqua>Warp <green>${name}</green> has been enabled.";
        public String warpDisabled = "<dark_aqua>Warp <green>${name}</green> has been disabled.";

        public String noWarp = "Warp does not exist.";
        public String notOwner = "You are not the owner of this warp!";

        public String worldCreateNotAllowed = "You cannot create a warp in this world!";
        public String worldMoveNotAllowed = "You cannot move warps into this world!";
        public String createUnsafe = "You cannot create a warp here!";
        public String moveUnsafe = "You cannot move warps here!";

        public String nameCensored = "<red>You can not use this name!";
        public String nameTaken = "<red>The name '${name}' is already used, try a different one!";
        public String nameTooLong = "<red>This name is too long. You can only use up to ${maxLength} characters!";
        public String nameTooShort = "<red>This name is too short, you need at least ${minLength} characters!";
        public String noPermissionToCreate = "<red>You cannot create any warps!";
        public String noPermissionToCreateMore = "<red>You cannot create more than ${max} warps!";

        public String warpMoveOnCooldown = "<red>You can move this warp again in ${duration}";
        public String unsafeTeleport = "<red>Teleport failed. Warp is unsafe to teleport to!";
        public String disabled = "<red>This warp is disabled!";
    }
}