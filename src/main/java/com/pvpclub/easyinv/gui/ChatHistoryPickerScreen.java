package com.pvpclub.easyinv.gui;

import com.pvpclub.easyinv.config.ModConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * "Pick a recent chat line" screen — the first half of the in-game
 * pattern builder. Shows the last {@link ModConfig#MAX_RECENT_CHAT_LINES}
 * lines captured by the chat mixin, newest at the top. Clicking a line
 * hands control to {@link ChatPatternBuilderScreen} for token tagging.
 *
 * <p>If no chat history has been captured yet (e.g. the user just
 * installed the mod), the screen shows a hint instead of an empty
 * list — the user needs to actually see some chat messages before
 * this tool can do anything useful.</p>
 */
public class ChatHistoryPickerScreen extends Screen {

    private static final int ROW_HEIGHT = 22;
    private static final int ROW_WIDTH = 360;

    private final Screen parent;
    private final List<String> lines;

    public ChatHistoryPickerScreen(Screen parent) {
        super(Text.literal("Pick a chat line"));
        this.parent = parent;
        // Copy + reverse so the newest line is on top, matching what
        // the user just saw scrolling up in chat.
        List<String> src = ModConfig.get().recentChatLines;
        this.lines = new ArrayList<>(src);
        Collections.reverse(this.lines);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 40;
        int x = centerX - ROW_WIDTH / 2;

        if (lines.isEmpty()) {
            // No history yet. Show an instruction row instead of buttons.
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("§7No chat history yet — send or receive a message in chat first, then come back§r"),
                    btn -> {} // no-op
            ).dimensions(x, startY, ROW_WIDTH, ROW_HEIGHT).build()).active = false;
        } else {
            int maxVisible = Math.min(lines.size(), (this.height - 100) / ROW_HEIGHT);
            for (int i = 0; i < maxVisible; i++) {
                String line = lines.get(i);
                String display = truncate(line, ROW_WIDTH / 6); // ~6 px per char estimate
                String marker = (i == 0) ? "§a» §r" : "§7  §r";
                final String picked = line;
                this.addDrawableChild(ButtonWidget.builder(
                        Text.literal(marker + display),
                        btn -> openBuilder(picked)
                ).dimensions(x, startY + i * ROW_HEIGHT, ROW_WIDTH, ROW_HEIGHT - 2).build());
            }
            if (lines.size() > maxVisible) {
                this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("§7… and " + (lines.size() - maxVisible) + " more (scroll up in chat)§r"),
                        btn -> {}
                ).dimensions(x, startY + maxVisible * ROW_HEIGHT, ROW_WIDTH, ROW_HEIGHT - 2).build()).active = false;
            }
        }

        // Bottom: Back / Cancel.
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Back"),
                btn -> close()
        ).dimensions(centerX - 50, this.height - 30, 100, 20).build());
    }

    private void openBuilder(String line) {
        if (this.client != null) {
            this.client.setScreen(new ChatPatternBuilderScreen(parent, line));
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    /**
     * Trim a raw chat line to fit the row width. We strip Minecraft
     * color codes first so they don't count against the visible budget.
     */
    private static String truncate(String line, int maxChars) {
        String stripped = line.replaceAll("§.", "");
        if (stripped.length() <= maxChars) {
            return stripped;
        }
        return stripped.substring(0, Math.max(0, maxChars - 1)) + "…";
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x80000000);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§7Pick the line that looks like a typical chat message on this server. You'll stamp its words with {name} / {message} next.§r"),
                this.width / 2, 26, 0xFFFFFF);
        super.render(ctx, mouseX, mouseY, delta);
    }
}