package me.wesley1808.playerwarps.gui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class SimplePagedGui extends SimpleGui {
    private final List<GuiElementInterface> elements = new ObjectArrayList<>();
    private final String title;
    private int maxPages;
    private int page;

    public SimplePagedGui(String title, ServerPlayer player) {
        super(MenuType.GENERIC_9x6, player, false);
        this.title = title;
        this.maxPages = 0;
        this.updateInventory();
    }

    public void setElements(List<GuiElementInterface> elements) {
        this.elements.clear();
        this.elements.addAll(elements);

        this.maxPages = (elements.size() - 1) / 28;
        this.updateInventory();
    }

    protected void updateInventory() {
        this.setTitle(Component.literal(String.format("%s | %d / %d", this.title, this.page + 1, this.maxPages + 1)));

        for (int index = 0; index < this.size; index++) {
            switch (index) {
                case 0 -> {
                    GuiElementBuilder builder = new GuiElementBuilder(Items.BARRIER);
                    builder.setName(Component.literal("Close").withStyle(ChatFormatting.DARK_RED));
                    builder.setCallback((i, type, clickType) -> {
                        GuiHelper.playSound(this.player, SoundEvents.UI_BUTTON_CLICK);
                        this.close();
                    });
                    this.setSlot(index, builder.build());
                }
                case 47 -> {
                    if (this.page > 0) {
                        GuiElementBuilder builder = new GuiElementBuilder(Items.ARROW);
                        builder.setName(Component.literal("Prev"));
                        builder.setCallback((i, type, clickType) -> {
                            GuiHelper.playSound(this.player, SoundEvents.UI_BUTTON_CLICK);
                            this.page--;
                            this.updateInventory();
                        });
                        this.setSlot(index, builder.build());
                    } else {
                        this.setSlot(index, GuiHelper.EMPTY_FILLER);
                    }
                }
                case 51 -> {
                    if (this.page < this.maxPages) {
                        GuiElementBuilder builder = new GuiElementBuilder(Items.ARROW);
                        builder.setName(Component.literal("Next"));
                        builder.setCallback((i, type, clickType) -> {
                            GuiHelper.playSound(this.player, SoundEvents.UI_BUTTON_CLICK);
                            this.page++;
                            this.updateInventory();
                        });
                        this.setSlot(index, builder.build());
                    } else {
                        this.setSlot(index, GuiHelper.EMPTY_FILLER);
                    }
                }
                default -> {
                    int mod = index % 9;
                    if (mod == 0 || mod == 8 || index < 9 || index > 44) {
                        this.setSlot(index, GuiHelper.EMPTY_FILLER);
                    } else {
                        int row = index / 9 - 1;
                        int id = mod + row * 7 - 1 + this.page * 28;
                        if (id < this.elements.size()) {
                            this.setSlot(index, this.elements.get(id));
                        } else {
                            this.setSlot(index, ItemStack.EMPTY);
                        }
                    }
                }
            }
        }
    }
}
