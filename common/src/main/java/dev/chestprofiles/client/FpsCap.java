package dev.chestprofiles.client;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import net.minecraft.client.Minecraft;

public final class FpsCap {

    private static final int CAP = 60;

    private static boolean active;
    private static int saved;

    private FpsCap() {
    }

    public static void begin() {
        if (active) {
            return;
        }
        FramerateLimitTracker tracker = Minecraft.getInstance().getFramerateLimitTracker();
        saved = tracker.getFramerateLimit();
        if (saved <= CAP) {
            return;
        }
        tracker.setFramerateLimit(CAP);
        active = true;
    }

    public static void end() {
        if (!active) {
            return;
        }
        Minecraft.getInstance().getFramerateLimitTracker().setFramerateLimit(saved);
        active = false;
    }
}
