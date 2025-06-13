package me.wesley1808.playerwarps.gui;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class GuiHelper {
    public static final ItemStack EMPTY_FILLER = emptyFiller();

    private static ItemStack emptyFiller() {
        ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        stack.set(DataComponents.CUSTOM_NAME, Component.empty());
        return stack;
    }

    public static void playSound(ServerPlayer player, Holder<SoundEvent> sound) {
        player.connection.send(new ClientboundSoundPacket(
                sound,
                SoundSource.RECORDS,
                player.getX(),
                player.getY(),
                player.getZ(),
                1,
                1,
                player.level().getRandom().nextLong()
        ));
    }
}
