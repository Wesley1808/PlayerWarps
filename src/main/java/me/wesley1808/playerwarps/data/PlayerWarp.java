package me.wesley1808.playerwarps.data;

import com.google.gson.annotations.Expose;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.wesley1808.playerwarps.config.Config;
import me.wesley1808.playerwarps.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlayerWarp extends Location implements Comparable<PlayerWarp> {
    private static final int VISIT_EXPIRE_TICKS = 20 * 60 * 60 * 2; // 2 hours
    private Object2IntOpenHashMap<UUID> visitTracker;
    @Expose
    @NotNull
    private final UUID owner;
    @Expose
    @NotNull
    private final String name;
    @Expose
    @Nullable
    private String description;
    @Expose
    @Nullable
    private Item icon;
    @Expose
    private int visits;
    @Expose
    private boolean disabled;
    @Expose
    private long lastMoved;

    public PlayerWarp(@NotNull UUID owner, @NotNull String name, ResourceLocation dim, BlockPos blockPos) {
        super(dim, blockPos);
        this.owner = owner;
        this.name = name;
        this.lastMoved = System.currentTimeMillis();
    }

    public void setIcon(@Nullable Item icon) {
        this.icon = icon;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void visit(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (this.owner.equals(uuid)) {
            return;
        }

        if (this.visitTracker == null) {
            this.visitTracker = new Object2IntOpenHashMap<>();
        }

        int currentTick = player.level().getServer().getTickCount();
        int lastVisitedTick = this.visitTracker.getOrDefault(uuid, -1);
        if (lastVisitedTick == -1 || currentTick - lastVisitedTick > VISIT_EXPIRE_TICKS) {
            this.visitTracker.put(uuid, currentTick);
            this.visits++;
        }
    }

    public void moveTo(ResourceLocation dim, BlockPos pos) {
        this.dimension = dim;
        this.blockPos = pos;
        this.lastMoved = System.currentTimeMillis();
    }

    public long remainingMoveCooldown() {
        long currentTime = System.currentTimeMillis();
        long cooldown = Config.instance().warpMoveCooldownSeconds * 1000L;
        return (this.lastMoved + cooldown) - currentTime;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public int getVisits() {
        return this.visits;
    }

    @NotNull
    public UUID getOwner() {
        return this.owner;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    @Nullable
    public Item getIcon() {
        return this.icon;
    }

    public String getOwnerName(MinecraftServer server) {
        return Util.asProfile(server, this.owner).map(GameProfile::getName).orElse(this.owner.toString());
    }

    @Override
    public int compareTo(@NotNull PlayerWarp other) {
        return Integer.compare(other.visits, this.visits);
    }
}
