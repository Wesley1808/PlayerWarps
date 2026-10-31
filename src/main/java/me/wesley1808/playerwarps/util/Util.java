package me.wesley1808.playerwarps.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.wesley1808.playerwarps.config.Config;
import me.wesley1808.teleportapi.api.TeleportManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public final class Util {

    public static Optional<NameAndId> asNameAndId(MinecraftServer server, UUID uuid) {
        return server.services().nameToIdCache().get(uuid);
    }

    public static CommandSyntaxException buildCommandException(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
    }

    public static <T, R> List<R> map(Collection<T> collection, Function<T, R> function) {
        List<R> result = new ObjectArrayList<>(collection.size());
        for (T value : collection) {
            R mapped = function.apply(value);
            if (mapped != null) {
                result.add(mapped);
            }
        }

        return result;
    }

    @Nullable
    public static String isNameAllowed(ServerPlayer player, String name) {
        int minLength = Config.instance().warpNameMinLength;
        int maxLength = Config.instance().warpNameMaxLength;

        if (name.length() > maxLength) {
            return Config.instance().messages.nameTooLong.replace("${maxLength}", String.valueOf(maxLength));
        }

        if (name.length() < minLength) {
            return Config.instance().messages.nameTooShort.replace("${minLength}", String.valueOf(minLength));
        }

        FilteredText text = player.getTextFilter().processStreamMessage(name).join();
        if (text.isFiltered()) {
            return Config.instance().messages.nameCensored;
        }

        return null;
    }

    public static String formatDuration(long duration) {
        return DurationFormatUtils.formatDuration(duration, "HH'h' mm'm' ss's'", false);
    }

    public static String toTitleCase(String id) {
        StringBuilder sb = new StringBuilder();
        for (String s : id.split("[_\\-\\s]")) {
            if (!s.isEmpty()) {
                sb.append(Character.toUpperCase(s.charAt(0)));
                sb.append(s.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    @Nullable
    public static String mayTeleport(ServerPlayer player) {
        if (TeleportManager.isTeleporting(player)) {
            return "You are already teleporting!";
        }

        if (player.gameMode.isSurvival() && Config.instance().useStrictTeleportCheck) {
            if (player.hasEffect(MobEffects.LEVITATION)) {
                return "You cannot teleport while levitating!";
            }

            if (player.hasEffect(MobEffects.DARKNESS)) {
                return "You cannot teleport while affected by darkness!";
            }

            return isBeingHunted(player, 24);
        }
        return null;
    }

    private static String isBeingHunted(ServerPlayer player, int huntRange) {
        List<Monster> monsters = player.level().getEntities(
                EntityTypeTest.forClass(Monster.class),
                player.getBoundingBox().inflate(64),
                EntitySelector.NO_SPECTATORS
        );

        int huntRangeSqr = huntRange * huntRange;
        for (Monster monster : monsters) {
            boolean isTargetedBy = monster.getTarget() == player || (
                    monster.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) &&
                    monster.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == player
            );

            if (!isTargetedBy && monster instanceof Warden warden) {
                isTargetedBy = warden.getAngerManagement().getActiveEntity().orElse(null) == player;
            }

            if (isTargetedBy && canHunt(monster, player, huntRangeSqr)) {
                return "You cannot teleport while there are monsters hunting you!";
            }
        }

        return null;
    }

    private static boolean canHunt(Monster monster, ServerPlayer player, int huntRangeSqr) {
        if (monster instanceof Warden) {
            return true;
        }

        return player.distanceToSqr(monster) < huntRangeSqr && monster.getSensing().hasLineOfSight(player);
    }
}
