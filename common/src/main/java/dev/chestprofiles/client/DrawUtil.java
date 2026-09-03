package dev.chestprofiles.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class DrawUtil {

    private DrawUtil() {
    }

    public static void drawRectBorder(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.horizontalLine(x1, x2, y1, color);
        graphics.horizontalLine(x1, x2, y2, color);
        graphics.verticalLine(x1, y1, y2, color);
        graphics.verticalLine(x2, y1, y2, color);
    }

    public static boolean contains(double mx, double my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx < x2 && my >= y1 && my < y2;
    }
}
