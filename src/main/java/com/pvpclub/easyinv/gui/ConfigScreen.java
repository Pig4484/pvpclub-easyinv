package com.pvpclub.easyinv.gui;

import com.pvpclub.easyinv.config.ModConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Vanilla-styled config screen for the mod. Wired into ModMenu via
 * {@link com.pvpclub.easyinv.integration.ModMenuIntegration}; also
 * accessible directly from code if a future keybind wants to open it.
 *
 * <p>The "Build chat pattern from recent chat…" button opens
 * {@link ChatHistoryPickerScreen}, the entry point for the
 * click-to-build-pattern wizard. Two fields are exposed at the top
 * so the wizard can pre-fill them and the user can hand-tweak the
 * result before pressing Save.</p>
 *
 * <p><b>v1.3.0:</b> removed the Require Modifier Key toggle and the
 * Modifier Key binding — clicks always fire on chat lines when the
 * mod is enabled and the player is on the configured server.</p>
 */
public class ConfigScreen extends Screen {

    private static final int ROW_HEIGHT = 30;
    private static final int ROW_WIDTH = 300;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen parent;
    private final ModConfig config;

    private TextFieldWidget addressField;
    private TextFieldWidget patternField;
    private TextFieldWidget commandField;
    private ButtonWidget enabledButton;
    private ButtonWidget serverOnlyButton;
    private ButtonWidget autoPatternButton;
    private ButtonWidget pickFromChatButton;

    public ConfigScreen(Screen parent) {
        super(Text.literal("PVP Club Easy Invite"));
        this.parent = parent;
        this.config = ModConfig.get();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 30;
        int x = centerX - ROW_WIDTH / 2;

        // --- Mod enabled (master switch) ---
        enabledButton = ButtonWidget.builder(
                enabledLabel(),
                button -> {
                    config.enabled = !config.enabled;
                    button.setMessage(enabledLabel());
                    refreshWidgetStates();
                }
        ).dimensions(x, startY, ROW_WIDTH, BUTTON_HEIGHT).build();
        this.addDrawableChild(enabledButton);

        // --- Server-only mode ---
        serverOnlyButton = ButtonWidget.builder(
                serverOnlyLabel(),
                button -> {
                    config.serverOnly = !config.serverOnly;
                    button.setMessage(serverOnlyLabel());
                    refreshWidgetStates();
                }
        ).dimensions(x, startY + ROW_HEIGHT, ROW_WIDTH, BUTTON_HEIGHT).build();
        this.addDrawableChild(serverOnlyButton);

        // --- Server address text field ---
        addressField = new TextFieldWidget(
                this.textRenderer,
                x, startY + ROW_HEIGHT * 2,
                ROW_WIDTH, BUTTON_HEIGHT,
                Text.literal("Server address")
        );
        addressField.setMaxLength(64);
        addressField.setText(config.serverAddress);
        this.addDrawableChild(addressField);

        // --- Chat pattern text field (used as fallback for unsigned chat) ---
        patternField = new TextFieldWidget(
                this.textRenderer,
                x, startY + ROW_HEIGHT * 3,
                ROW_WIDTH, BUTTON_HEIGHT,
                Text.literal("Chat pattern")
        );
        patternField.setMaxLength(128);
        patternField.setText(config.chatPattern);
        patternField.setSuggestion("{name} » {message}");
        this.addDrawableChild(patternField);

        // --- "Build chat pattern from recent chat" wizard entry point ---
        pickFromChatButton = ButtonWidget.builder(
                Text.literal("Build chat pattern from recent chat…"),
                button -> {
                    if (this.client != null) {
                        this.client.setScreen(new ChatHistoryPickerScreen(this));
                    }
                }
        ).dimensions(x, startY + (int)(ROW_HEIGHT * 3.5), ROW_WIDTH, BUTTON_HEIGHT).build();
        this.addDrawableChild(pickFromChatButton);

        // --- Invite command template ---
        commandField = new TextFieldWidget(
                this.textRenderer,
                x, startY + ROW_HEIGHT * 4,
                ROW_WIDTH, BUTTON_HEIGHT,
                Text.literal("Invite command")
        );
        commandField.setMaxLength(128);
        commandField.setText(config.commandPattern);
        commandField.setSuggestion("/p invite {name}");
        this.addDrawableChild(commandField);

        // --- Auto-attach from pattern toggle ---
        autoPatternButton = ButtonWidget.builder(
                autoPatternLabel(),
                button -> {
                    config.autoAttachFromPattern = !config.autoAttachFromPattern;
                    button.setMessage(autoPatternLabel());
                }
        ).dimensions(x, startY + ROW_HEIGHT * 5, ROW_WIDTH, BUTTON_HEIGHT).build();
        this.addDrawableChild(autoPatternButton);

        // --- Done / Cancel ---
        int bottomY = this.height - 30;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Cancel"),
                button -> {
                    ModConfig.reload();
                    close();
                }
        ).dimensions(centerX - 154, bottomY, 150, BUTTON_HEIGHT).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Save & Close"),
                button -> {
                    String addr = addressField.getText();
                    if (addr != null && !addr.isBlank()) {
                        config.serverAddress = addr.trim();
                    }
                    String pat = patternField.getText();
                    if (pat != null && !pat.isBlank()) {
                        config.chatPattern = pat;
                    }
                    String cmd = commandField.getText();
                    if (cmd != null && !cmd.isBlank()) {
                        config.commandPattern = cmd;
                    }
                    config.save();
                    close();
                }
        ).dimensions(centerX + 4, bottomY, 150, BUTTON_HEIGHT).build());

        refreshWidgetStates();
    }

    // ---- Widget enable / disable state -----------------------------------

    private void refreshWidgetStates() {
        boolean enabled = config.enabled;
        enabledButton.active = true;

        serverOnlyButton.active = enabled;
        addressField.setEditable(enabled && config.serverOnly);
        addressField.active = enabled && config.serverOnly;

        patternField.setEditable(enabled);
        patternField.active = enabled;

        pickFromChatButton.active = enabled;
        commandField.setEditable(enabled);
        commandField.active = enabled;

        autoPatternButton.active = enabled;
    }

    // ---- Navigation -------------------------------------------------------

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        } else {
            super.close();
        }
    }

    // ---- Render ----------------------------------------------------------

    @Override
    public void render(net.minecraft.client.gui.DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x80000000);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);

        // Section labels.
        int labelX = this.width / 2 - ROW_WIDTH / 2;
        int labelYBase = 18;
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7Master switch"), labelX, labelYBase, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7Server filter"), labelX, labelYBase + ROW_HEIGHT, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7Chat pattern (fallback for unsigned chat)"),
                labelX, labelYBase + ROW_HEIGHT * 3, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7Invite command"),
                labelX, labelYBase + ROW_HEIGHT * 4, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7Trigger"), labelX,
                labelYBase + ROW_HEIGHT * 5, 0xFFFFFF);

        super.render(ctx, mouseX, mouseY, delta);
    }

    // ---- Button labels ------------------------------------------------------

    private Text enabledLabel() {
        return Text.literal("Mod Enabled: " + onOff(config.enabled));
    }

    private Text serverOnlyLabel() {
        return Text.literal("Only on this server: " + onOff(config.serverOnly));
    }

    private Text autoPatternLabel() {
        return Text.literal("Auto-attach from pattern: " + onOff(config.autoAttachFromPattern));
    }

    private static String onOff(boolean b) {
        return b ? "§aON§r" : "§cOFF§r";
    }
}