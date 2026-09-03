package dev.chestprofiles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public final class SlotLock {

    public static final int INVENTORY_SIZE = 36;
    private static final int HOTBAR_SIZE = 9;
    private static final int LOCK_COLOR = 0xFFFF55FF;

    private static final boolean[] locked = new boolean[INVENTORY_SIZE];
    private static boolean initialized;

    private static void ensureInitialized() {
        if (!initialized) {
            initialized = true;
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                locked[i] = true;
            }
        }
    }

    private SlotLock() {
    }

    public static boolean isLocked(int slotIndex) {
        return slotIndex >= 0 && slotIndex < INVENTORY_SIZE && locked[slotIndex];
    }

    public static boolean isLockedSlot(Slot slot, Inventory inventory) {
        if (slot.container != inventory || !slot.isActive()) {
            return false;
        }
        return isLocked(slot.getContainerSlot());
    }

    public static void toggle(int slotIndex) {
        ensureInitialized();
        if (slotIndex >= 0 && slotIndex < INVENTORY_SIZE) {
            locked[slotIndex] = !locked[slotIndex];
        }
    }

    public static boolean isAltDown() {
        return Minecraft.getInstance().hasAltDown();
    }

    public static boolean clickToggle(AbstractContainerScreen<?> screen, int leftPos, int topPos, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        Inventory inventory = minecraft.player.getInventory();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container != inventory || !slot.isActive()) {
                continue;
            }
            int index = slot.getContainerSlot();
            if (index < 0 || index >= INVENTORY_SIZE) {
                continue;
            }
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            if (DrawUtil.contains(mouseX, mouseY, x, y, x + 16, y + 16)) {
                toggle(index);
                return true;
            }
        }
        return false;
    }

    public static void render(GuiGraphicsExtractor graphics, int leftPos, int topPos, AbstractContainerMenu menu) {
        ensureInitialized();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        Inventory inventory = minecraft.player.getInventory();
        for (Slot slot : menu.slots) {
            if (slot.container != inventory || !slot.isActive()) {
                continue;
            }
            int index = slot.getContainerSlot();
            if (!isLocked(index)) {
                continue;
            }
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            DrawUtil.drawRectBorder(graphics, x - 1, y - 1, x + 16, y + 16, LOCK_COLOR);
        }
    }
}
