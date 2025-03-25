package me.wesley1808.playerwarps.util;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.TicketType;

public final class RegistryUtil {
    public static final TicketType PRE_TELEPORT = registerTicketType("playerwarps:pre_teleport", 70L, false, TicketType.TicketUse.LOADING);

    public static void register() {
    }

    private static TicketType registerTicketType(String id, long timeOut, boolean persists, TicketType.TicketUse ticketUse) {
        return Registry.register(BuiltInRegistries.TICKET_TYPE, id, new TicketType(timeOut, persists, ticketUse));
    }
}
