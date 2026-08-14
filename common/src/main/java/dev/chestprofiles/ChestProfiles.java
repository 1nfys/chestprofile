package dev.chestprofiles;

import com.mojang.logging.LogUtils;
import dev.chestprofiles.client.config.ProfileConfig;
import org.slf4j.Logger;

public final class ChestProfiles {
    public static final String MOD_ID = "chestprofile";
    public static final String MOD_NAME = "Chest Profile";
    private static final Logger LOG = LogUtils.getLogger();

    private ChestProfiles() {
    }

    public static void init() {
        ProfileConfig.load();
        LOG.info("[ChestProfile] initialized (version 1.0, Minecraft 26.2)");
    }
}
