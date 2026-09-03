package dev.chestprofiles.client.engine;

import com.mojang.logging.LogUtils;
import dev.chestprofiles.client.SlotLock;
import dev.chestprofiles.client.config.ProfileConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class TransferEngine {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int STEPS_PER_TICK = 5;
    private static final int MAX_STEPS = 6000;

    private static TransferEngine activeEngine;

    private final AbstractContainerScreen<?> screen;
    private int currentFilterIndex;
    private Integer sourceSlotIndex;
    private int stepCount;

    private TransferEngine(AbstractContainerScreen<?> screen) {
        this.screen = screen;
    }

    public static boolean canOperate(AbstractContainerScreen<?> screen) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || screen == null || minecraft.player.isSpectator()) {
            return false;
        }
        if (screen instanceof CreativeModeInventoryScreen || screen instanceof InventoryScreen) {
            return false;
        }
        if (!(screen.getMenu() instanceof ChestMenu)) {
            return false;
        }
        return hasExternalContainer(screen);
    }

    public static boolean hasExternalContainer(AbstractContainerScreen<?> screen) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        Inventory inventory = minecraft.player.getInventory();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container != inventory) {
                return true;
            }
        }
        return false;
    }

    public static boolean isActiveFor(AbstractContainerScreen<?> screen) {
        return activeEngine != null && activeEngine.screen == screen;
    }

    public static void tick(AbstractContainerScreen<?> screen) {
        if (activeEngine == null) {
            return;
        }
        for (int i = 0; i < STEPS_PER_TICK; i++) {
            if (activeEngine == null) {
                return;
            }
            if (activeEngine.screen != screen || Minecraft.getInstance().gui.screen() != screen
                    || !canOperate(screen)) {
                activeEngine = null;
                return;
            }
            if (activeEngine.stepCount >= MAX_STEPS) {
                LOGGER.info("Transfer aborted: step limit reached");
                activeEngine = null;
                return;
            }
            activeEngine.step();
        }
    }

    public static void onScreenClosed(AbstractContainerScreen<?> screen) {
        if (activeEngine != null && activeEngine.screen == screen) {
            activeEngine = null;
        }
    }

    public static boolean startFill(AbstractContainerScreen<?> screen) {
        if (!canOperate(screen) || activeEngine != null) {
            return false;
        }
        ProfileConfig.Config config = ProfileConfig.instance.getActiveConfig();
        ProfileConfig.Profile profile = ProfileConfig.activeProfileOf(config);
        if (profile == null) {
            return false;
        }
        activeEngine = new TransferEngine(screen);
        return true;
    }

    private void step() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        AbstractContainerMenu menu = screen.getMenu();
        if (player == null) {
            activeEngine = null;
            return;
        }
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) {
            stepPickPhase(player, menu);
        } else {
            stepPlacePhase(player, menu, carried);
        }
    }

    private void stepPickPhase(LocalPlayer player, AbstractContainerMenu menu) {
        ProfileConfig.Config config = ProfileConfig.instance.getActiveConfig();
        ProfileConfig.Profile profile = ProfileConfig.activeProfileOf(config);
        if (profile == null) {
            activeEngine = null;
            return;
        }

        for (int entryIndex = currentFilterIndex; entryIndex < profile.items.size(); entryIndex++) {
            ProfileConfig.ItemEntry entry = profile.items.get(entryIndex);
            Item item = ProfileConfig.itemFromId(entry.item);
            if (item == null) {
                continue;
            }
            Slot source = findPlayerItemSlot(menu, player, item);
            if (source == null) {
                continue;
            }
            click(menu, source);
            sourceSlotIndex = source.index;
            currentFilterIndex = entryIndex;
            return;
        }

        activeEngine = null;
    }

    private void stepPlacePhase(LocalPlayer player, AbstractContainerMenu menu, ItemStack carried) {
        Slot target = findLayoutSlot(menu, player, carried);
        if (target == null) {
            returnCarried(menu, player);
            sourceSlotIndex = null;
            activeEngine = null;
            return;
        }
        click(menu, target);
        if (menu.getCarried().isEmpty()) {
            sourceSlotIndex = null;
        }
    }

    public static List<Slot> chestSlots(AbstractContainerMenu menu, Inventory playerInventory) {
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.container != playerInventory && slot.isActive()) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private static boolean canMerge(Slot slot, ItemStack carried) {
        if (!slot.hasItem() || !slot.mayPlace(carried)) {
            return false;
        }
        ItemStack current = slot.getItem();
        return ItemStack.isSameItemSameComponents(current, carried)
                && current.getCount() < current.getMaxStackSize()
                && current.getCount() < slot.getMaxStackSize();
    }

    private Slot findLayoutSlot(AbstractContainerMenu menu, LocalPlayer player, ItemStack carried) {
        ProfileConfig.Profile profile = ProfileConfig.instance.getActiveProfile();
        if (profile == null) {
            return null;
        }
        Inventory inventory = player.getInventory();
        List<Slot> chestSlots = chestSlots(menu, inventory);
        int[] stacksPerEntry = ProfileConfig.stacksPerEntry(profile, inventory, chestSlots, carried);
        List<ProfileConfig.ItemEntry> layout = ProfileConfig.scaledLayout(profile, stacksPerEntry, chestSlots.size());

        int regionEnd = -1;
        for (int slotIndex = 0; slotIndex < layout.size(); slotIndex++) {
            ProfileConfig.ItemEntry entry = layout.get(slotIndex);
            Item item = ProfileConfig.itemFromId(entry.item);
            if (item == null || item != carried.getItem()) {
                continue;
            }
            regionEnd = Math.max(regionEnd, slotIndex + 1);
            if (slotIndex < chestSlots.size()) {
                Slot slot = chestSlots.get(slotIndex);
                if (canMerge(slot, carried)) {
                    return slot;
                }
            }
        }

        for (int slotIndex = 0; slotIndex < layout.size(); slotIndex++) {
            ProfileConfig.ItemEntry entry = layout.get(slotIndex);
            Item item = ProfileConfig.itemFromId(entry.item);
            if (item == null || item != carried.getItem()) {
                continue;
            }
            if (slotIndex < chestSlots.size()) {
                Slot slot = chestSlots.get(slotIndex);
                if (slot.mayPlace(carried) && slot.getItem().isEmpty()) {
                    return slot;
                }
            }
        }

        for (Slot slot : chestSlots) {
            if (canMerge(slot, carried)) {
                return slot;
            }
        }

        int startIndex = Math.max(0, regionEnd);
        for (Slot slot : chestSlots) {
            if (slot.getContainerSlot() >= startIndex && slot.getItem().isEmpty() && slot.mayPlace(carried)) {
                return slot;
            }
        }

        for (Slot slot : chestSlots) {
            if (slot.getItem().isEmpty() && slot.mayPlace(carried)) {
                return slot;
            }
        }
        return null;
    }

    private Slot findPlayerItemSlot(AbstractContainerMenu menu, LocalPlayer player, Item item) {
        if (item == null) {
            return null;
        }
        Inventory inventory = player.getInventory();
        for (Slot slot : menu.slots) {
            if (slot.container == inventory && slot.isActive()
                    && !SlotLock.isLockedSlot(slot, inventory)
                    && slot.hasItem() && slot.getItem().getItem() == item
                    && slot.mayPickup(player)) {
                return slot;
            }
        }
        return null;
    }

    private Slot findPlayerPlacementSlot(AbstractContainerMenu menu, LocalPlayer player, ItemStack carried) {
        Inventory inventory = player.getInventory();
        for (Slot slot : menu.slots) {
            if (slot.container != inventory || !slot.isActive() || SlotLock.isLockedSlot(slot, inventory)
                    || !slot.hasItem()
                    || !ItemStack.isSameItemSameComponents(slot.getItem(), carried)
                    || slot.getItem().getCount() >= slot.getItem().getMaxStackSize()
                    || slot.getItem().getCount() >= slot.getMaxStackSize()
                    || !slot.mayPlace(carried)) {
                continue;
            }
            return slot;
        }
        for (Slot slot : menu.slots) {
            if (slot.container == inventory && slot.isActive() && !SlotLock.isLockedSlot(slot, inventory)
                    && slot.getItem().isEmpty() && slot.mayPlace(carried)) {
                return slot;
            }
        }
        return null;
    }

    private void returnCarried(AbstractContainerMenu menu, LocalPlayer player) {
        if (sourceSlotIndex != null) {
            for (Slot slot : menu.slots) {
                if (slot.index == sourceSlotIndex && slot.isActive()
                        && !SlotLock.isLockedSlot(slot, player.getInventory())) {
                    click(menu, slot);
                    if (menu.getCarried().isEmpty()) {
                        return;
                    }
                    break;
                }
            }
        }
        if (!menu.getCarried().isEmpty() && player != null) {
            Slot fallback = findPlayerPlacementSlot(menu, player, menu.getCarried());
            if (fallback != null) {
                click(menu, fallback);
            }
        }
    }

    private void click(AbstractContainerMenu menu, Slot slot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null || minecraft.player == null) {
            activeEngine = null;
            return;
        }
        minecraft.gameMode.handleContainerInput(menu.containerId, slot.index, 0, ContainerInput.PICKUP, minecraft.player);
        stepCount++;
    }
}
