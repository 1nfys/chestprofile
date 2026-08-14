package dev.chestprofiles.neoforge;

import dev.chestprofiles.ChestProfiles;
import dev.chestprofiles.client.KeybindManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(ChestProfiles.MOD_ID)
public final class ChestProfilesNeoForge {

    public ChestProfilesNeoForge(IEventBus modEventBus) {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            ChestProfiles.init();
            if (modEventBus != null) {
                modEventBus.addListener(this::onRegisterKeyMappings);
            }
        }
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeybindManager.TRANSFER);
        event.register(KeybindManager.TOGGLE_PANEL);
    }
}
