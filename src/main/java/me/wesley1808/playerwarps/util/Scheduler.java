package me.wesley1808.playerwarps.util;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.wesley1808.playerwarps.config.Config;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler {
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(0);
    private static final ObjectOpenHashSet<UUID> ACTIVE = new ObjectOpenHashSet<>();

    public static boolean canSchedule(UUID uuid) {
        return !ACTIVE.contains(uuid);
    }

    public static void schedule(long millis, Runnable runnable) {
        SCHEDULER.schedule(runnable, millis, TimeUnit.MILLISECONDS);
    }

    public static void shutdown() {
        SCHEDULER.shutdown();
    }

    public static void scheduleTeleport(ServerPlayer player, Runnable onSuccess, Runnable onFail) {
        ACTIVE.add(player.getUUID());
        teleportLoop(3, player.server, player.getUUID(), player.position(), onSuccess, onFail);
    }

    private static void teleportLoop(int seconds, MinecraftServer server, UUID uuid, Vec3 oldPos, Runnable onSuccess, Runnable onFail) {
        if (seconds <= 0) {
            ACTIVE.remove(uuid);
            server.execute(onSuccess);
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player == null || !player.position().closerThan(oldPos, 2)) {
            ACTIVE.remove(uuid);
            server.execute(onFail);
            return;
        }

        player.displayClientMessage(Formatter.parse(Config.instance().messages.tpSecondsLeft
                .replace("${seconds}", String.valueOf(seconds))
                .replace("seconds", seconds == 1 ? "second" : "seconds")
        ), false);

        schedule(1000, () -> teleportLoop(seconds - 1, server, uuid, oldPos, onSuccess, onFail));
    }
}