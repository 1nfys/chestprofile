package dev.chestprofiles.client.gui;

import dev.chestprofiles.client.DrawUtil;
import dev.chestprofiles.client.config.ProfileConfig;
import dev.chestprofiles.client.engine.TransferEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

public final class ProfilePanel {

    public static final int MARGIN = 4;
    public static final int HEADER_H = 18;
    public static final int SCROLLBAR_W = 2;
    private static final int FILL_W = 13;
    private static final int FILL_H = 13;
    private static final int FILL_GAP = 2;
    private static final int DEFAULT_FILL_X_OFFSET = 3;
    private static final int DEFAULT_FILL_Y_OFFSET = 18;

    private static final int ROW_H = 18;
    private static final int MAX_ROWS = 11;
    private static final int TEXTURE_W = 176;
    private static final int TEXTURE_H = 222;
    private static final int CONTENT_INSET_X = 7;
    private static final int ARROW_W = 13;
    private static final int PREV_DY = 3;
    private static final int NEXT_DY = 3;
    private static final int GEAR_DY = 3;

    private static final int PANEL_TRIM_LEFT = 1;
    private static final int BOTTOM_BORDER_H = 8;
    private static final int OUTLINE_BOTTOM_TRIM = 2;
    private static final float ROW_ITEM_SCALE = 0.75f;

    private static final int PANEL_OFFSET_X = MARGIN - PANEL_TRIM_LEFT;
    private static final int PANEL_H = TEXTURE_H;
    private static final int GRID_Y1_OFFSET = TEXTURE_H - BOTTOM_BORDER_H;
    private static final int PREV_X_OFFSET = 7;
    private static final int NEXT_X_OFFSET = 37;
    private static final int GEAR_X_OFFSET = 20;
    private static final int NAME_X0_OFFSET = 24;
    private static final int NAME_X1_OFFSET = 41;
    private static final int ROW_INSET_LEFT = CONTENT_INSET_X + 1;
    private static final int ROW_INSET_RIGHT = CONTENT_INSET_X + SCROLLBAR_W;

    private static final int TOOLTIP_PAD = 4;
    private static final int TOOLTIP_MAX_ICONS = 12;
    private static final int TOOLTIP_ITEM_STEP = 18;
    private static final int TOOLTIP_BOX_H = TOOLTIP_PAD * 2 + 29;

    private static final int BUTTON_BG = 0x99000000;
    private static final int BUTTON_HOVER = 0xCC000000;
    private static final int LINE_COLOR = 0x44FFFFFF;
    private static final int HOVER_BORDER = 0xFFFFFFFF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SCROLL_TRACK = 0x22000000;
    private static final int SCROLL_THUMB = 0xAAFFFFFF;

    private static final int ROW_COLOR = 0x22000000;
    private static final int ROW_SELECTED_FILL = 0x6600AA00;
    private static final int ROW_SELECTED_BORDER = 0xFF00FF00;

    private static final int PHANTOM_MATCH = 0xFF44FF55;

    private final AbstractContainerScreen<?> screen;
    private int leftPos;
    private int topPos;
    private int imageWidth;
    private int imageHeight;
    private int scroll;
    private boolean draggingScroll;
    private int scrollGrabOffset;
    private String chestKey;

    public ProfilePanel(AbstractContainerScreen<?> screen) {
        this.screen = screen;
    }

    public void setPosition(int leftPos, int topPos, int imageWidth, int imageHeight) {
        this.leftPos = leftPos;
        this.topPos = topPos;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public void setChestKey(String chestKey) {
        this.chestKey = chestKey;
    }

    public float panelScale() {
        float availableWidth = Math.max(20.0f, (float) (leftPos - MARGIN));
        float availableHeight = Math.max(20.0f, (float) (screen.height - 8));
        float horizontalScale = availableWidth / (float) imageWidth;
        float verticalScale = availableHeight / (float) PANEL_H;
        return Math.max(0.3f, Math.min(1.0f, Math.min(horizontalScale, verticalScale)));
    }

    public int scaledX0() {
        float scale = panelScale();
        return (int) (leftPos - MARGIN - imageWidth * scale);
    }

    public int scaledY0() {
        float scale = panelScale();
        float scaledPanelHeight = PANEL_H * scale;
        return (int) Math.max(4, Math.min(topPos, screen.height - 4 - scaledPanelHeight));
    }

    private int toLocalX(double mouseX) {
        float scale = panelScale();
        return (int) (x0() + (mouseX - scaledX0()) / scale);
    }

    private int toLocalY(double mouseY) {
        float scale = panelScale();
        return (int) (y0() + (mouseY - scaledY0()) / scale);
    }

    public int x0() {
        return leftPos - imageWidth - PANEL_OFFSET_X;
    }

    public int y0() {
        return topPos;
    }

    public int x1() {
        return x0() + imageWidth;
    }

    public int y1() {
        return y0() + PANEL_H;
    }

    public boolean isVisible() {
        return ProfileConfig.instance.panelEnabled && TransferEngine.canOperate(screen);
    }

    public boolean contains(int mouseX, int mouseY) {
        float scale = panelScale();
        int scaledX = scaledX0();
        int scaledY = scaledY0();
        return DrawUtil.contains(mouseX, mouseY, scaledX, scaledY,
                (int) (scaledX + imageWidth * scale), (int) (scaledY + PANEL_H * scale));
    }

    private int gridY0() {
        return y0() + HEADER_H;
    }

    private int gridY1() {
        return y0() + GRID_Y1_OFFSET;
    }

    private int visibleRows() {
        int count = listedProfiles().size();
        int available = gridY1() - gridY0();
        int rows = (available + ROW_H - 1) / ROW_H;
        return Math.max(1, Math.min(MAX_ROWS, Math.min(count, rows)));
    }

    private int rowX0() {
        return x0() + ROW_INSET_LEFT;
    }

    private int rowX1() {
        return x1() - ROW_INSET_RIGHT;
    }

    private int rowY(int index) {
        return gridY0() + (index - scroll) * ROW_H;
    }

    private List<Integer> listedProfiles() {
        List<Integer> list = new ArrayList<>();
        ProfileConfig.Config config = ProfileConfig.instance.getActiveConfig();
        if (config != null) {
            for (int i = 0; i < config.profiles.size(); i++) {
                ProfileConfig.Profile profile = config.profiles.get(i);
                if (profile != null && !profile.items.isEmpty()) {
                    list.add(i);
                }
            }
        }
        return list;
    }

    private int maxScroll() {
        return Math.max(0, listedProfiles().size() - visibleRows());
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private void jumpScroll(int localY) {
        int maxScrollOffset = maxScroll();
        if (maxScrollOffset <= 0) {
            return;
        }
        int trackHeight = scrollbarTrack();
        int thumbHeight = scrollbarThumbSize();
        int travelDistance = Math.max(1, trackHeight - thumbHeight);
        int relativeOffset = Math.max(0, Math.min(travelDistance, localY - gridY0()));
        scroll = relativeOffset * maxScrollOffset / travelDistance;
        clampScroll();
    }

    private int listedIndexAt(int localX, int localY) {
        if (localX < rowX0() || localX >= rowX1() || localY < gridY0() || localY >= gridY1()) {
            return -1;
        }
        int index = scroll + (localY - gridY0()) / ROW_H;
        return index >= 0 && index < listedProfiles().size() ? index : -1;
    }

    private boolean overButton(int localX, int localY, int bx, int by) {
        return DrawUtil.contains(localX, localY, bx, by, bx + ARROW_W, by + ARROW_W);
    }

    private boolean overPrev(int localX, int localY) {
        return overButton(localX, localY, x0() + PREV_X_OFFSET, y0() + PREV_DY);
    }

    private boolean overNext(int localX, int localY) {
        return overButton(localX, localY, x1() - NEXT_X_OFFSET, y0() + NEXT_DY);
    }

    private boolean overGear(int localX, int localY) {
        return overButton(localX, localY, x1() - GEAR_X_OFFSET, y0() + GEAR_DY);
    }

    private boolean overHiddenGear(int mouseX, int mouseY) {
        if (ProfileConfig.instance.panelEnabled || !TransferEngine.canOperate(screen)) {
            return false;
        }
        return overButton(mouseX, mouseY, x1() - GEAR_X_OFFSET, y0() + GEAR_DY);
    }

    public int fillX0() {
        int[] below = clientsortLastButton();
        if (below != null) {
            return below[0];
        }
        return leftPos + imageWidth - DEFAULT_FILL_X_OFFSET;
    }

    public int fillX1() {
        return fillX0() + FILL_W;
    }

    public int fillY0() {
        int[] below = clientsortLastButton();
        if (below != null) {
            return below[1] + below[3] + FILL_GAP;
        }
        return topPos + DEFAULT_FILL_Y_OFFSET;
    }

    public int fillY1() {
        return fillY0() + FILL_H;
    }

    private int[] clientsortLastButton() {
        int[] lastButtonBounds = null;
        for (GuiEventListener listener : screen.children()) {
            if (!(listener instanceof Button button)) {
                continue;
            }
            if (button.getWidth() < 10 || button.getWidth() > 18 || button.getHeight() < 10 || button.getHeight() > 18) {
                continue;
            }
            int buttonX = button.getX();
            int buttonY = button.getY();
            if (buttonX < leftPos + imageWidth - 24 || buttonX > leftPos + imageWidth + 16) {
                continue;
            }
            if (buttonY >= topPos + imageHeight / 2) {
                continue;
            }
            if (lastButtonBounds == null || buttonY > lastButtonBounds[1]) {
                lastButtonBounds = new int[] { buttonX, buttonY, button.getWidth(), button.getHeight() };
            }
        }
        return lastButtonBounds;
    }

    private boolean overFillButton(int mouseX, int mouseY) {
        return DrawUtil.contains(mouseX, mouseY, fillX0(), fillY0(), fillX1(), fillY1());
    }

    private boolean overScrollbar(int localX, int localY) {
        return localX >= x1() - ROW_INSET_RIGHT && localX < x1() - CONTENT_INSET_X
                && localY >= gridY0() && localY < scrollbarY1();
    }

    private static Item itemFor(ProfileConfig.ItemEntry entry) {
        return entry == null ? null : ProfileConfig.itemFromId(entry.item);
    }

    private void openSettings() {
        Minecraft.getInstance().gui.setScreen(new SettingsScreen(screen));
    }

    public void renderBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!TransferEngine.canOperate(screen)) {
            return;
        }
        if (!ProfileConfig.instance.panelEnabled) {
            renderHiddenGear(graphics, mouseX, mouseY);
            return;
        }
        clampScroll();

        float scale = panelScale();
        int scaledX = scaledX0();
        int scaledY = scaledY0();
        int localMouseX = toLocalX(mouseX);
        int localMouseY = toLocalY(mouseY);

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(scaledX, scaledY);
        pose.scale(scale, scale);
        pose.translate(-x0(), -y0());

        int panelX = x0();
        int panelY = y0();
        Identifier backgroundTexture = Identifier.fromNamespaceAndPath("chestprofile", "textures/gui/container/interface.png");
        graphics.blit(RenderPipelines.GUI_TEXTURED, backgroundTexture, panelX, panelY, 0.0f, 0.0f,
                TEXTURE_W, PANEL_H, TEXTURE_W, PANEL_H, TEXTURE_W, TEXTURE_H);

        renderRows(graphics, localMouseX, localMouseY);
        renderHeader(graphics, localMouseX, localMouseY);
        renderScrollbar(graphics, localMouseX, localMouseY);

        pose.popMatrix();
    }

    private void renderHiddenGear(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int panelRight = x1();
        int panelY = y0();
        boolean hovered = overHiddenGear(mouseX, mouseY);
        int gearX = panelRight - GEAR_X_OFFSET;
        int gearY = panelY + GEAR_DY;
        graphics.fill(gearX, gearY, gearX + ARROW_W, gearY + ARROW_W, hovered ? BUTTON_HOVER : BUTTON_BG);
        renderGear(graphics, gearX, gearY, hovered);
    }

    public void renderFillButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!TransferEngine.canOperate(screen)) {
            return;
        }
        String spriteName = "fill";
        if (!fillEnabled()) {
            spriteName = "fill_disabled";
        } else if (TransferEngine.isActiveFor(screen) || overFillButton(mouseX, mouseY)) {
            spriteName = "fill_highlighted";
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                Identifier.fromNamespaceAndPath("chestprofile", spriteName),
                fillX0(), fillY0(), FILL_W, FILL_H, 0xFFFFFFFF);
    }

    private boolean fillEnabled() {
        if (!TransferEngine.canOperate(screen)) {
            return false;
        }
        return ProfileConfig.instance.getActiveProfile() != null;
    }

    private void renderHeader(GuiGraphicsExtractor graphics, int localMouseX, int localMouseY) {
        Font font = Minecraft.getInstance().font;
        int panelX = x0();
        int panelY = y0();
        int panelRight = x1();
        int prevArrowY = panelY + PREV_DY;
        int nextArrowY = panelY + NEXT_DY;

        drawArrow(graphics, panelX + PREV_X_OFFSET, prevArrowY, true, overPrev(localMouseX, localMouseY));
        drawArrow(graphics, panelRight - NEXT_X_OFFSET, nextArrowY, false, overNext(localMouseX, localMouseY));

        renderGear(graphics, panelRight - GEAR_X_OFFSET, panelY + GEAR_DY, overGear(localMouseX, localMouseY));

        ProfileConfig.Config config = ProfileConfig.instance.getActiveConfig();
        String configName = config == null ? "" : config.name;
        int nameLeft = panelX + NAME_X0_OFFSET;
        int nameRight = panelRight - NAME_X1_OFFSET;
        int maxNameWidth = Math.max(8, nameRight - nameLeft);
        String clippedName = font.plainSubstrByWidth(configName, maxNameWidth);
        graphics.text(font, clippedName, nameLeft + (maxNameWidth - font.width(clippedName)) / 2,
                panelY + PREV_DY + Math.max(0, (ARROW_W - font.lineHeight) / 2), TEXT_COLOR, true);
    }

    private void drawArrow(GuiGraphicsExtractor graphics, int buttonX, int buttonY, boolean left, boolean hovered) {
        String spriteName = left ? (hovered ? "arrow_left_h" : "arrow_left") : (hovered ? "arrow_right_h" : "arrow_right");
        Identifier identifier = Identifier.fromNamespaceAndPath("chestprofile", spriteName);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, buttonX, buttonY, ARROW_W, ARROW_W, 0xFFFFFFFF);
    }

    private void renderGear(GuiGraphicsExtractor graphics, int gearX, int gearY, boolean hovered) {
        String spriteName = hovered ? "cog_h" : "cog";
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                Identifier.fromNamespaceAndPath("chestprofile", spriteName),
                gearX, gearY, ARROW_W, ARROW_W, 0xFFFFFFFF);
    }

    private void renderRows(GuiGraphicsExtractor graphics, int localMouseX, int localMouseY) {
        Font font = Minecraft.getInstance().font;
        ProfileConfig.Config config = ProfileConfig.instance.getActiveConfig();
        List<Integer> profileIndices = listedProfiles();
        int hoveredRowIndex = listedIndexAt(localMouseX, localMouseY);
        for (int index = 0; index < profileIndices.size(); index++) {
            int rowOffset = index - scroll;
            if (rowOffset < 0 || rowOffset >= visibleRows()) {
                continue;
            }
            int profileIndex = profileIndices.get(index);
            int rowLeft = rowX0();
            int rowRight = rowX1();
            int rowTop = rowY(index);
            boolean isSelected = config != null && config.activeProfile == profileIndex;
            int fillBottom = Math.min(rowTop + ROW_H, gridY1());
            graphics.fill(rowLeft, rowTop, rowRight, fillBottom, isSelected ? ROW_SELECTED_FILL : ROW_COLOR);
            int outlineBottom = Math.min(rowTop + ROW_H - OUTLINE_BOTTOM_TRIM, gridY1());
            if (isSelected) {
                DrawUtil.drawRectBorder(graphics, rowLeft - 1, rowTop - 1, rowRight + 1, outlineBottom, ROW_SELECTED_BORDER);
            } else if (index == hoveredRowIndex) {
                DrawUtil.drawRectBorder(graphics, rowLeft - 1, rowTop - 1, rowRight + 1, outlineBottom, HOVER_BORDER);
            } else if (rowTop + ROW_H <= gridY1()) {
                graphics.horizontalLine(rowLeft, rowRight, rowTop + ROW_H - 1, LINE_COLOR);
            }
            ProfileConfig.Profile profile = config != null ? config.profiles.get(profileIndex) : null;
            if (profile == null || profile.items.isEmpty()) {
                continue;
            }
            ProfileConfig.ItemEntry firstEntry = profile.items.get(0);
            Item item = itemFor(firstEntry);
            if (item == null) {
                continue;
            }
            int displayCount = Math.min(firstEntry.count, item.getDefaultInstance().getMaxStackSize());
            ItemStack renderStack = new ItemStack(item, Math.max(1, displayCount));
            float iconX = rowLeft + 3.0f;
            float iconY = rowTop + 3.0f;
            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(iconX, iconY);
            pose.scale(ROW_ITEM_SCALE, ROW_ITEM_SCALE);
            graphics.fakeItem(renderStack, 0, 0, profileIndex);
            if (displayCount > 1) {
                graphics.itemDecorations(font, renderStack, 0, 0);
            }
            pose.popMatrix();

            String title = profile.name != null && !profile.name.isBlank()
                    ? profile.name : item.getName(renderStack).getString();
            String label = profileIndex + "  " + title + " (" + ProfileConfig.profileSlots(profile) + ")";
            int textX = rowLeft + 19;
            int maxLabelWidth = Math.max(8, rowRight - 2 - textX);
            String clippedLabel = font.plainSubstrByWidth(label, maxLabelWidth);
            graphics.text(font, clippedLabel, textX, rowTop + 5, TEXT_COLOR, true);
        }
    }

    private int scrollbarY1() {
        return gridY1();
    }

    private int scrollbarTrack() {
        return scrollbarY1() - gridY0();
    }

    private int scrollbarThumbSize() {
        int maxScrollOffset = maxScroll();
        int trackHeight = scrollbarTrack();
        return Math.max(10, trackHeight / (maxScrollOffset + 1));
    }

    private int scrollbarThumbTop() {
        int maxScrollOffset = maxScroll();
        int trackHeight = scrollbarTrack();
        int thumbHeight = scrollbarThumbSize();
        return gridY0() + (trackHeight - thumbHeight) * scroll / Math.max(1, maxScrollOffset);
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics, int localMouseX, int localMouseY) {
        int maxScrollOffset = maxScroll();
        if (maxScrollOffset <= 0) {
            return;
        }
        int scrollbarLeft = x1() - ROW_INSET_RIGHT;
        int scrollbarBottom = scrollbarY1();
        graphics.fill(scrollbarLeft, gridY0(), x1() - CONTENT_INSET_X, scrollbarBottom, SCROLL_TRACK);
        int thumbHeight = scrollbarThumbSize();
        int thumbY = scrollbarThumbTop();
        boolean isHovered = overScrollbar(localMouseX, localMouseY);
        graphics.fill(scrollbarLeft, thumbY, x1() - CONTENT_INSET_X, thumbY + thumbHeight, isHovered ? 0xEEFFFFFF : SCROLL_THUMB);
    }

    public void renderOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!isVisible()) {
            return;
        }
        int localMouseX = toLocalX(mouseX);
        int localMouseY = toLocalY(mouseY);
        renderTooltip(graphics, mouseX, mouseY, localMouseX, localMouseY);
    }

    private List<ProfileConfig.ItemEntry> buildAssignment(ProfileConfig.Profile profile, Inventory inventory) {
        List<Slot> chestSlots = TransferEngine.chestSlots(screen.getMenu(), inventory);
        ItemStack carried = screen.getMenu().getCarried();
        int[] stacksPerEntry = ProfileConfig.stacksPerEntry(profile, inventory, chestSlots, carried);
        return ProfileConfig.scaledLayout(profile, stacksPerEntry, chestSlots.size());
    }

    public void renderChestPhantoms(GuiGraphicsExtractor graphics) {
        if (!isVisible()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        Inventory inventory = minecraft.player.getInventory();
        ProfileConfig.Config config = ProfileConfig.instance.getActiveConfig();
        ProfileConfig.Profile profile = ProfileConfig.activeProfileOf(config);
        if (profile == null) {
            return;
        }

        List<ProfileConfig.ItemEntry> assignment = buildAssignment(profile, inventory);

        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == inventory || !slot.isActive()) {
                continue;
            }
            int containerSlotIndex = slot.getContainerSlot();
            if (containerSlotIndex < 0 || containerSlotIndex >= assignment.size()) {
                continue;
            }
            ProfileConfig.ItemEntry entry = assignment.get(containerSlotIndex);
            Item item = itemFor(entry);
            if (item == null) {
                continue;
            }
            int slotX = leftPos + slot.x;
            int slotY = topPos + slot.y;
            ItemStack currentStack = slot.getItem();
            if (!currentStack.isEmpty()) {
                if (ProfileConfig.profileContains(profile, currentStack)) {
                    drawPhantomBorder(graphics, slotX, slotY);
                }
                continue;
            }
            drawPhantomBorder(graphics, slotX, slotY);
            ItemStack renderStack = new ItemStack(item,
                    Math.max(1, Math.min(entry.count, item.getDefaultInstance().getMaxStackSize())));
            graphics.fakeItem(renderStack, slotX, slotY, slot.index);
            graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x60000000);
            if (renderStack.getCount() > 1) {
                graphics.itemDecorations(minecraft.font, renderStack, slotX, slotY);
            }
        }
    }

    private void drawPhantomBorder(GuiGraphicsExtractor graphics, int x, int y) {
        DrawUtil.drawRectBorder(graphics, x - 1, y - 1, x + 16, y + 16, PHANTOM_MATCH);
    }

    private void renderTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int localMouseX, int localMouseY) {
        int index = listedIndexAt(localMouseX, localMouseY);
        if (index < 0) {
            return;
        }
        ProfileConfig.Config config = ProfileConfig.instance.getActiveConfig();
        if (config == null) {
            return;
        }
        int profileIndex = listedProfiles().get(index);
        ProfileConfig.Profile profile = config.profiles.get(profileIndex);
        Font font = Minecraft.getInstance().font;

        List<ItemStack> itemStacks = new ArrayList<>();
        for (ProfileConfig.ItemEntry entry : profile.items) {
            Item item = itemFor(entry);
            if (item == null) {
                continue;
            }
            int count = Math.min(Math.max(1, entry.count), item.getDefaultInstance().getMaxStackSize());
            itemStacks.add(new ItemStack(item, count));
        }
        if (itemStacks.isEmpty()) {
            return;
        }

        String headerText = "Profile " + profileIndex
                + (profile.name != null && !profile.name.isBlank() ? " §o" + profile.name : "")
                + "  §7" + ProfileConfig.profileSlots(profile) + " slots";

        int displayedIconCount = Math.min(TOOLTIP_MAX_ICONS, itemStacks.size());
        String moreLabel = itemStacks.size() > displayedIconCount ? "+" + (itemStacks.size() - displayedIconCount) + " more" : null;
        int iconRowWidth = displayedIconCount * TOOLTIP_ITEM_STEP - 2;
        int moreWidth = moreLabel == null ? 0 : displayedIconCount * TOOLTIP_ITEM_STEP + 2 + font.width(moreLabel);
        int contentWidth = Math.max(font.width(headerText), Math.max(iconRowWidth, moreWidth));
        int boxWidth = contentWidth + TOOLTIP_PAD * 2 + 2;

        int tooltipX = mouseX + 8;
        if (tooltipX + boxWidth > screen.width - 2) {
            tooltipX = mouseX - boxWidth - 8;
        }
        int tooltipY = mouseY + 4;
        if (tooltipY + TOOLTIP_BOX_H > screen.height - 2) {
            tooltipY = mouseY - TOOLTIP_BOX_H - 4;
        }

        graphics.fill(tooltipX, tooltipY, tooltipX + boxWidth, tooltipY + TOOLTIP_BOX_H, 0xF0100010);
        graphics.horizontalLine(tooltipX, tooltipX + boxWidth - 1, tooltipY, 0xFF909090);
        graphics.horizontalLine(tooltipX, tooltipX + boxWidth - 1, tooltipY + TOOLTIP_BOX_H - 1, 0xFF202020);
        graphics.verticalLine(tooltipX, tooltipY, tooltipY + TOOLTIP_BOX_H - 1, 0xFF606060);
        graphics.verticalLine(tooltipX + boxWidth - 1, tooltipY, tooltipY + TOOLTIP_BOX_H - 1, 0xFF202020);
        graphics.text(font, headerText, tooltipX + TOOLTIP_PAD + 1, tooltipY + TOOLTIP_PAD, 0xFFFFFFFF, false);

        int iconX = tooltipX + TOOLTIP_PAD + 1;
        int iconY = tooltipY + TOOLTIP_PAD + 9 + 4;
        for (int i = 0; i < displayedIconCount; i++) {
            ItemStack stack = itemStacks.get(i);
            graphics.fakeItem(stack, iconX, iconY, -1);
            if (stack.getCount() > 1) {
                graphics.itemDecorations(font, stack, iconX, iconY);
            }
            iconX += TOOLTIP_ITEM_STEP;
        }
        if (moreLabel != null) {
            graphics.text(font, moreLabel, iconX + 2, iconY + 4, 0xFFB266FF, false);
        }
    }

    public boolean fillButtonClicked(double mouseX, double mouseY, int button) {
        if (!TransferEngine.canOperate(screen) || button != 0) {
            return false;
        }
        int clickX = (int) mouseX;
        int clickY = (int) mouseY;
        if (!overFillButton(clickX, clickY)) {
            return false;
        }
        if (!TransferEngine.isActiveFor(screen)) {
            TransferEngine.startFill(screen);
        }
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int localMouseX = toLocalX(mouseX);
        int localMouseY = toLocalY(mouseY);
        if (button == 0 && overHiddenGear((int) mouseX, (int) mouseY)) {
            openSettings();
            return true;
        }
        if (!isVisible()) {
            return false;
        }
        if (!contains((int) mouseX, (int) mouseY)) {
            return false;
        }
        if (TransferEngine.isActiveFor(screen)) {
            return true;
        }
        if (button == 0) {
            if (overScrollbar(localMouseX, localMouseY) && maxScroll() > 0) {
                int thumbTop = scrollbarThumbTop();
                if (localMouseY >= thumbTop && localMouseY < thumbTop + scrollbarThumbSize()) {
                    draggingScroll = true;
                    scrollGrabOffset = localMouseY - thumbTop;
                } else {
                    jumpScroll(localMouseY);
                }
                return true;
            }
            if (overPrev(localMouseX, localMouseY)) {
                ProfileConfig config = ProfileConfig.instance;
                config.cycleConfig(false);
                config.selectChestProfile(chestKey, config.activeConfigIndex, -1);
                return true;
            }
            if (overNext(localMouseX, localMouseY)) {
                ProfileConfig config = ProfileConfig.instance;
                config.cycleConfig(true);
                config.selectChestProfile(chestKey, config.activeConfigIndex, -1);
                return true;
            }
            if (overGear(localMouseX, localMouseY)) {
                openSettings();
                return true;
            }
        }
        int index = listedIndexAt(localMouseX, localMouseY);
        if (index >= 0) {
            int profileIndex = listedProfiles().get(index);
            if (button == 0) {
                ProfileConfig config = ProfileConfig.instance;
                if (config.getActiveConfig() != null) {
                    config.selectChestProfile(chestKey, config.activeConfigIndex, profileIndex);
                }
                return true;
            }
            if (button == 1) {
                ProfileConfig config = ProfileConfig.instance;
                if (config.getActiveConfig() != null) {
                    config.selectChestProfile(chestKey, config.activeConfigIndex, -1);
                }
                return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isVisible() || !contains((int) mouseX, (int) mouseY)) {
            return false;
        }
        int delta = scrollY > 0 ? -1 : 1;
        scroll = Math.max(0, Math.min(scroll + delta, maxScroll()));
        return true;
    }

    public boolean mouseDragged(MouseButtonEvent event) {
        if (!draggingScroll || !isVisible()) {
            return false;
        }
        int maxScrollOffset = maxScroll();
        if (maxScrollOffset <= 0) {
            return false;
        }
        int localMouseY = toLocalY(event.y());
        int travelDistance = Math.max(1, scrollbarTrack() - scrollbarThumbSize());
        int relativeOffset = Math.max(0, Math.min(travelDistance, localMouseY - gridY0() - scrollGrabOffset));
        scroll = relativeOffset * maxScrollOffset / travelDistance;
        clampScroll();
        return true;
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingScroll) {
            draggingScroll = false;
            return true;
        }
        return false;
    }
}
