package com.pvpclub.easyinv.gui;

import com.pvpclub.easyinv.config.ModConfig;
import com.pvpclub.easyinv.config.ModifierKey;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Vanilla-styled config screen for the mod. Wired into ModMenu via
 * {@link com.pvpclub.easyinv.integration.ModMenuIntegration}; also
 * accessible directly from code if a future keybind wants to open it.
 *
 * <p>The "Modifier Key" row uses the same "press to bind" pattern as
 * Minecraft's vanilla controls screen: clicking the button arms the
 * screen, the next key/mouse button press becomes the new binding,
 * and pressing {@code Esc} cancels.</p>
 */
public class ConfigScreen extends Screen {

    private static final int ROW_HEIGHT = 30;
    private static final int ROW_WIDTH = 300;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen parent;
    private final ModConfig config;

    private TextFieldWidget addressField;
    private TextFieldWidget patternField;
    private ButtonWidget enabledButton;
    private ButtonWidget serverOnlyButton;
    private ButtonWidget autoPatternButton;
    private ButtonWidget requireModifierButton;
    private ButtonWidget modifierKeyButton;

    /**
     * True while the screen is waiting for the user to press a key or
     * click a mouse button to assign it as the modifier key.
     */
    private boolean waitingForKey = false;

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

        // --- Auto-attach from pattern toggle ---
        autoPatternButton = ButtonWidget.builder(
                autoPatternLabel(),
                button -> {
                    config.autoAttachFromPattern = !config.autoAttachFromPattern;
                    button.setMessage(autoPatternLabel());
                }
        ).dimensions(x, startY + ROW_HEIGHT * 4, ROW_WIDTH, BUTTON_HEIGHT).build();
        this.addDrawableChild(autoPatternButton);

        // --- Require modifier key ---
        requireModifierButton = ButtonWidget.builder(
                requireModifierLabel(),
                button -> {
                    config.requireModifierKey = !config.requireModifierKey;
                    button.setMessage(requireModifierLabel());
                    refreshWidgetStates();
                }
        ).dimensions(x, startY + ROW_HEIGHT * 5, ROW_WIDTH, BUTTON_HEIGHT).build();
        this.addDrawableChild(requireModifierButton);

        // --- Modifier Key — press-to-bind, vanilla controls style ---
        modifierKeyButton = ButtonWidget.builder(
                modifierKeyLabel(),
                button -> {
                    waitingForKey = !waitingForKey;
                    button.setMessage(modifierKeyLabel());
                }
        ).dimensions(x, startY + ROW_HEIGHT * 6, ROW_WIDTH, BUTTON_HEIGHT).build();
        this.addDrawableChild(modifierKeyButton);

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
                    config.save();
                    close();
                }
        ).dimensions(centerX + 4, bottomY, 150, BUTTON_HEIGHT).build());

        refreshWidgetStates();
    }

    // ---- Key / mouse capture while arming the modifier-key button -------

    @Override
    public boolean keyPressed(KeyInput input) {
        if (waitingForKey) {
            int code = input.getKeycode();
            // Esc cancels the binding.
            if (code == GLFW.GLFW_KEY_ESCAPE) {
                waitingForKey = false;
                modifierKeyButton.setMessage(modifierKeyLabel());
                return true;
            }
            acceptModifierKey(code);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(Click click, boolean isClient) {
        if (waitingForKey) {
            // Mouse buttons come in as their GLFW button code
            // (0 = left, 1 = right, 2 = middle, …).
            int code = click.button();
            // The "release" of a held click can also be reported as a
            // mouseClicked; ignore codes that aren't real buttons.
            if (code >= 0 && code <= 7) {
                acceptModifierKey(code);
                return true;
            }
            waitingForKey = false;
            modifierKeyButton.setMessage(modifierKeyLabel());
            return true;
        }
        return super.mouseClicked(click, isClient);
    }

    private void acceptModifierKey(int glfwCode) {
        ModifierKey picked = ModifierKey.ofGlfwCode(glfwCode);
        config.setModifierKey(picked);
        waitingForKey = false;
        modifierKeyButton.setMessage(modifierKeyLabel());
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

        autoPatternButton.active = enabled;
        requireModifierButton.active = enabled;
        modifierKeyButton.active = enabled;
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
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7Trigger"), labelX,
                labelYBase + ROW_HEIGHT * 5, 0xFFFFFF);

        // While we're armed for key-capture, draw a centred prompt so
        // the user knows what's going on even if the button is small.
        if (waitingForKey) {
            String prompt = "Press a key (or Esc to cancel)";
            int promptY = this.height / 2 + 80;
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("§e" + prompt + "§r"), this.width / 2, promptY, 0xFFFFFF);
        }

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

    private Text requireModifierLabel() {
        return Text.literal("Require Modifier Key: " + onOff(config.requireModifierKey));
    }

    private Text modifierKeyLabel() {
        if (waitingForKey) {
            return Text.literal("§eModifier Key: §6[ Press a key… ]§r");
        }
        ModifierKey key = config.getModifierKey();
        String display = key.isUnbound() ? "§7[None]§r" : key.getDisplayName();
        return Text.literal("Modifier Key: " + display);
    }

    private static String onOff(boolean b) {
        return b ? "§aON§r" : "§cOFF§r";
    }
}