package dev.chestprofiles.fabric;

import dev.chestprofiles.ChestProfiles;
import dev.chestprofiles.client.KeybindManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public final class ChestProfilesFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ChestProfiles.init();
        KeyMappingHelper.registerKeyMapping(KeybindManager.TRANSFER);
        KeyMappingHelper.registerKeyMapping(KeybindManager.TOGGLE_PANEL);
    }
}
