package me.wesley1808.playerwarps.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.wesley1808.playerwarps.config.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
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

    public static boolean isOnline(ServerPlayer player) {
        return player.level().getServer().getPlayerList().getPlayer(player.getUUID()) != null;
    }

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

    @Nullable
    public static String mayTeleport(ServerPlayer player) {
        if (player.gameMode.isSurvival() && Config.instance().useStrictTeleportCheck) {
            if (player.hasEffect(MobEffects.LEVITATION)) {
                return "Levitation Effect";
            }

            if (player.hasEffect(MobEffects.DARKNESS)) {
                return "Darkness Effect";
            }

            List<Monster> monsters = player.level().getEntities(EntityTypeTest.forClass(Monster.class), player.getBoundingBox().inflate(64D), EntitySelector.NO_SPECTATORS);
            for (Monster monster : monsters) {
                if (isTargeted(player, monster)) {
                    if (monster instanceof Warden) {
                        return "Hunted by warden";
                    }

                    float distance = player.distanceTo(monster);
                    if (distance < 24 && monster.getSensing().hasLineOfSight(player)) {
                        return String.format("Hunted by %s (%.0f blocks away)", EntityType.getKey(monster.getType()).getPath(), distance);
                    }
                }
            }
        }

        return null;
    }

    public static boolean isTargeted(ServerPlayer target, Mob mob) {
        boolean isTargeted = mob.getTarget() == target;

        // Check the memory for mobs that don't use the target selector.
        if (!isTargeted) {
            Brain<?> brain = mob.getBrain();
            MemoryModuleType<?> module = MemoryModuleType.ATTACK_TARGET;
            isTargeted = brain.hasMemoryValue(module) && brain.getMemory(module).orElse(null) == target;
        }

        // Check if the mob is a warden that is sniffing out the player.
        if (!isTargeted && mob instanceof Warden warden) {
            isTargeted = warden.getAngerManagement().getActiveEntity().orElse(null) == target;
        }

        return isTargeted;
    }

    public static String formatDuration(long duration) {
        return DurationFormatUtils.formatDuration(duration, "HH'h' mm'm' ss's'", false);
    }
}
