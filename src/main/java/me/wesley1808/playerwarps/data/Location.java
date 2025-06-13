package me.wesley1808.playerwarps.data;

import com.google.gson.annotations.Expose;
import me.wesley1808.playerwarps.config.Config;
import me.wesley1808.playerwarps.util.Formatter;
import me.wesley1808.playerwarps.util.RegistryUtil;
import me.wesley1808.playerwarps.util.Scheduler;
import me.wesley1808.playerwarps.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Predicate;

public class Location {
    @Expose
    public ResourceLocation dimension;
    @Expose
    public BlockPos blockPos;

    public Location(ResourceLocation dim, BlockPos blockPos) {
        this.dimension = dim;
        this.blockPos = blockPos;
    }

    public void teleport(ServerPlayer player, boolean movementCheck, @Nullable Location.TeleportPredicate predicate) {
        ServerLevel level = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, this.dimension));
        if (level == null) {
            player.sendSystemMessage(Component.literal(String.format("Invalid dimension! %s", this.dimension)).withStyle(ChatFormatting.RED));
            return;
        }

        if (movementCheck) {
            level.getChunkSource().addTicketWithRadius(RegistryUtil.PRE_TELEPORT, new ChunkPos(this.blockPos), 1);

            Scheduler.scheduleTeleport(player, () -> {
                if (Util.isOnline(player)) {
                    this.teleport(player, level, predicate);
                }
            }, () -> {
                player.displayClientMessage(Formatter.parse(Config.instance().messages.tpCancelled), false);
            });
        } else {
            this.teleport(player, level, predicate);
        }
    }

    private void teleport(ServerPlayer player, ServerLevel world, @Nullable Location.TeleportPredicate predicate) {
        if (predicate != null && predicate.test.test(world)) {
            predicate.onError.run();
            return;
        }

        player.teleportTo(world, this.blockPos.getX() + 0.5D, this.blockPos.getY(), this.blockPos.getZ() + 0.5D, Set.of(), player.getYRot(), player.getXRot(), true);
        player.connection.resetPosition();
    }

    public record TeleportPredicate(Predicate<ServerLevel> test, Runnable onError) {
    }
}
