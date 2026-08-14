package dev.chestprofiles.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.chestprofiles.ChestProfiles;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class KeybindManager {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(ChestProfiles.MOD_ID, "main")
    );

    public static final KeyMapping TRANSFER = new KeyMapping(
            "key.chestprofile.transfer",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    public static final KeyMapping TOGGLE_PANEL = new KeyMapping(
            "key.chestprofile.togglePanel",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    private KeybindManager() {
    }
}
