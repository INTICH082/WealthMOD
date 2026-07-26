package com.mcwealth.mod.gui;

import com.mcwealth.mod.network.ForbesData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

public final class ForbesScreen extends Screen {

    private static final int ROW_HEIGHT = 16;

    private final List<ForbesData.Entry> entries;
    private int scrollOffset = 0;
    private int listTop;
    private int listBottom;

    public ForbesScreen(ForbesData data) {
        super(Text.translatable("gui.minecraftwealth.forbes.title"));
        this.entries = data.entries();
    }

    @Override
    protected void init() {
        listTop = 32;
        listBottom = height - 34;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(width / 2 - 50, height - 28, 100, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);

        int panelLeft = width / 6;
        int panelRight = width - width / 6;

        context.enableScissor(panelLeft, listTop, panelRight, listBottom);
        int y = listTop - scrollOffset;
        for (int i = 0; i < entries.size(); i++) {
            if (y + ROW_HEIGHT >= listTop && y <= listBottom) {
                drawRow(context, entries.get(i), panelLeft, panelRight, y, mouseX, mouseY);
            }
            y += ROW_HEIGHT;
        }
        context.disableScissor();
    }

    private void drawRow(DrawContext context, ForbesData.Entry entry, int left, int right, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= left && mouseX <= right && mouseY >= y && mouseY < y + ROW_HEIGHT;
        if (hovered) {
            context.fill(left, y, right, y + ROW_HEIGHT, 0x40FFFFFF);
        } else if (entry.rank() % 2 == 0) {
            context.fill(left, y, right, y + ROW_HEIGHT, 0x22000000);
        }

        int rankColor = entry.rank() == 1 ? 0xFFD700 : entry.rank() == 2 ? 0xC0C0C0 : entry.rank() == 3 ? 0xCD7F32 : 0xDDDDDD;
        context.drawText(textRenderer, Text.literal("#" + entry.rank()), left + 4, y + 4, rankColor, false);
        context.drawText(textRenderer, Text.literal(entry.playerName()), left + 44, y + 4, 0xFFFFFF, false);
        String wealth = "$" + String.format(Locale.ROOT, "%,.2f", entry.wealth());
        int wealthWidth = textRenderer.getWidth(wealth);
        context.drawText(textRenderer, Text.literal(wealth), right - wealthWidth - 4, y + 4, 0x55FF55, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelLeft = width / 6;
        int panelRight = width - width / 6;
        if (mouseX >= panelLeft && mouseX <= panelRight && mouseY >= listTop && mouseY <= listBottom) {
            int index = (int) ((mouseY - listTop + scrollOffset) / ROW_HEIGHT);
            if (index >= 0 && index < entries.size()) {
                ForbesData.Entry entry = entries.get(index);
                if (client != null && client.player != null) {
                    client.player.networkHandler.sendChatCommand("wealth graph " + entry.playerName());
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int contentHeight = entries.size() * ROW_HEIGHT;
        int visibleHeight = listBottom - listTop;
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (verticalAmount * ROW_HEIGHT * 2)));
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}