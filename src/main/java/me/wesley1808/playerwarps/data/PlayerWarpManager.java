package me.wesley1808.playerwarps.data;

import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.wesley1808.playerwarps.PlayerWarps;
import me.wesley1808.playerwarps.config.Config;
import me.wesley1808.playerwarps.config.ConfigManager;
import me.wesley1808.playerwarps.config.Json;
import me.wesley1808.playerwarps.util.Permission;
import me.wesley1808.playerwarps.util.Util;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public final class PlayerWarpManager {
    private static final File FILE = new File(FabricLoader.getInstance().getGameDir().toFile(), "playerwarps.json");
    private static Object2ObjectOpenHashMap<String, PlayerWarp> playerWarps = new Object2ObjectOpenHashMap<>();
    private static boolean loaded;

    public static Object2ObjectMap<String, PlayerWarp> getWarps() {
        return Object2ObjectMaps.unmodifiable(playerWarps);
    }

    public static List<PlayerWarp> getWarps(Predicate<PlayerWarp> predicate) {
        ObjectArrayList<PlayerWarp> warps = new ObjectArrayList<>();
        for (PlayerWarp warp : playerWarps.values()) {
            if (predicate.test(warp)) {
                warps.add(warp);
            }
        }
        return warps;
    }

    public static PlayerWarp getWarpByName(String warpName) {
        return playerWarps.get(warpName);
    }

    public static PlayerWarp getOwnedWarpOrException(ServerPlayer player, String warpName) throws CommandSyntaxException {
        PlayerWarp warp = getWarpOrException(warpName);
        if (!warp.getOwner().equals(player.getUUID()) && !Permissions.check(player, Permission.ADMIN)) {
            throw Util.buildCommandException(Config.instance().messages.notOwner);
        }

        return warp;
    }

    public static PlayerWarp getWarpOrException(String warpName) throws CommandSyntaxException {
        PlayerWarp warp = playerWarps.get(warpName);
        if (warp == null) {
            throw Util.buildCommandException(Config.instance().messages.noWarp);
        }

        return warp;
    }

    public static boolean add(UUID owner, String name, ResourceLocation dim, BlockPos blockPos) {
        return playerWarps.putIfAbsent(name, new PlayerWarp(owner, name, dim, blockPos)) == null;
    }

    public static void remove(String name) {
        playerWarps.remove(name);
    }

    public static boolean isUnsafe(ServerLevel level, BlockPos blockPos) {
        LevelChunk chunk = level.getChunkAt(blockPos);
        BlockState belowState = chunk.getBlockState(blockPos.below());
        BlockState headState = chunk.getBlockState(blockPos.above());
        BlockState feetState = chunk.getBlockState(blockPos);
        Block below = belowState.getBlock();
        Block headBlock = headState.getBlock();
        Block feetBlock = feetState.getBlock();

        // Block below feet must have collision, not be a tall block (player will fall through) and not damage the player.
        return !below.hasCollision || below instanceof FenceBlock || below instanceof FenceGateBlock || below instanceof WallBlock
               || below == Blocks.CACTUS || below == Blocks.MAGMA_BLOCK || belowState.is(BlockTags.CAMPFIRES)
               // Block at feet height must not have collision and cannot be lava or fire.
               || feetBlock.hasCollision || feetBlock == Blocks.LAVA || feetState.is(BlockTags.FIRE)
               // Block at head height must not have collision and cannot be a liquid (drowning).
               || headBlock.hasCollision || headState.liquid();
    }

    public static void save() {
        if (loaded) {
            try (var writer = ConfigManager.getWriter(FILE)) {
                writer.write(Json.PLAYER_WARPS.toJson(playerWarps));
            } catch (Exception ex) {
                PlayerWarps.LOGGER.error("Failed to save player warps!", ex);
            }
        } else {
            PlayerWarps.LOGGER.warn("Attempted to save player warps before they were loaded!");
        }
    }

    public static void load() {
        if (FILE.exists()) {
            try (var reader = ConfigManager.getReader(FILE)) {
                playerWarps = Json.PLAYER_WARPS.fromJson(reader, new TypeToken<Object2ObjectOpenHashMap<String, PlayerWarp>>() {}.getType());
            } catch (Exception ex) {
                PlayerWarps.LOGGER.error("Failed to load player warps!", ex);
            }
        }
        loaded = true;
    }
}
