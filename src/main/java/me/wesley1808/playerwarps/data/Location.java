package me.wesley1808.playerwarps.data;

import com.google.gson.annotations.Expose;
import me.wesley1808.teleportapi.api.TeleportBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;

public class Location {
    @Expose
    public Identifier dimension;
    @Expose
    public BlockPos blockPos;

    public Location(Identifier dim, BlockPos blockPos) {
        this.dimension = dim;
        this.blockPos = blockPos;
    }

    public void teleport(TeleportBuilder builder) {
        ServerLevel level = builder.player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, this.dimension));
        if (level == null) {
            builder.player.sendSystemMessage(Component.literal(String.format("Invalid dimension! %s", this.dimension)).withStyle(ChatFormatting.RED));
            return;
        }

        builder.to(level, Vec3.atBottomCenterOf(this.blockPos));
        builder.relatives(Relative.ROTATION);
        builder.execute();
    }
}
