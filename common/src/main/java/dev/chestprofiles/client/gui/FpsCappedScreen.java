package dev.chestprofiles.client.gui;

import dev.chestprofiles.client.FpsCap;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class FpsCappedScreen extends Screen {

    protected FpsCappedScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        FpsCap.begin();
        super.init();
    }

    @Override
    public void removed() {
        FpsCap.end();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
