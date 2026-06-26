package com.pvpclub.easyinv.integration;

import com.pvpclub.easyinv.gui.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

/**
 * Bridges the mod into Mod Menu so a "Mods" → "PVP Club Easy Invite" →
 * "Config" button shows up. ModMenu is declared as {@code suggests} in
 * fabric.mod.json and as {@code compileOnly} in build.gradle, so the mod
 * still loads cleanly without ModMenu installed.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}