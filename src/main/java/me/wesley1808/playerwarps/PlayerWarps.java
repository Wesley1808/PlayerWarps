package me.wesley1808.playerwarps;

import com.mojang.logging.LogUtils;
import me.wesley1808.playerwarps.command.PlayerWarpCommand;
import me.wesley1808.playerwarps.config.ConfigManager;
import me.wesley1808.playerwarps.data.PlayerWarpManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;

public class PlayerWarps implements ModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        boolean generated = ConfigManager.load();
        if (!generated) {
            ConfigManager.save();
        }

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PlayerWarpManager.load();
        });

        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> {
            PlayerWarpManager.save();
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
            PlayerWarpCommand.register(dispatcher, buildContext);
        });
    }
}
