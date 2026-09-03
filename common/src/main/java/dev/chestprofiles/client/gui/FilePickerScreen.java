package dev.chestprofiles.client.gui;

import dev.chestprofiles.client.config.ProfileConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class FilePickerScreen extends FpsCappedScreen {

    public enum Mode { LOAD, SAVE }

    private static final int LIST_HALF_WIDTH = 150;
    private static final int LIST_WIDTH = 300;
    private static final int LIST_START_Y = 56;
    private static final int ROW_HEIGHT = 20;
    private static final int DRIVES_START_Y = 34;
    private static final int DRIVE_HEIGHT = 16;
    private static final int BOTTOM_BAR_HEIGHT = 28;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ACTION_BUTTON_WIDTH = 60;

    private final Mode mode;
    private final String defaultName;
    private final Consumer<Path> onOpen;
    private final Consumer<Path> onSave;
    private final Screen returnTo;

    private Path currentDirectory;
    private Path selectedPath;
    private EditBox nameInputBox;
    private final List<Path> entries = new ArrayList<>();
    private final List<Path> drives = new ArrayList<>();
    private int scrollOffset;
    private String errorMessage;

    public FilePickerScreen(Mode mode, String defaultName, Consumer<Path> onOpen, Consumer<Path> onSave, Screen returnTo) {
        super(Component.translatable(mode == Mode.LOAD
                ? "chestprofile.screen.openFile" : "chestprofile.screen.saveFile"));
        this.mode = mode;
        this.defaultName = defaultName;
        this.onOpen = onOpen;
        this.onSave = onSave;
        this.returnTo = returnTo;
        this.currentDirectory = ProfileConfig.CONFIG_DIR;
    }

    @Override
    protected void init() {
        refresh();
        int centerX = this.width / 2;
        int bottomButtonY = this.height - BOTTOM_BAR_HEIGHT;

        addRenderableWidget(Button.builder(Component.literal(".."), button -> navigate(currentDirectory.getParent()))
                .bounds(4, 4, 40, BUTTON_HEIGHT).build());

        if (mode == Mode.SAVE) {
            this.nameInputBox = addRenderableWidget(new EditBox(this.font, centerX - LIST_HALF_WIDTH, bottomButtonY, 200, BUTTON_HEIGHT,
                    Component.translatable("chestprofile.screen.name")));
            this.nameInputBox.setMaxLength(64);
            if (this.defaultName != null) {
                this.nameInputBox.setValue(this.defaultName);
            }
            this.nameInputBox.setFocused(true);
            addRenderableWidget(Button.builder(Component.translatable("chestprofile.button.save"), button -> savePressed())
                    .bounds(centerX + 58, bottomButtonY, ACTION_BUTTON_WIDTH, BUTTON_HEIGHT).build());
            addRenderableWidget(Button.builder(Component.translatable("chestprofile.button.cancel"), button -> this.onClose())
                    .bounds(centerX + 122, bottomButtonY, ACTION_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("chestprofile.button.open"), button -> openSelected())
                    .bounds(centerX + 58, bottomButtonY, ACTION_BUTTON_WIDTH, BUTTON_HEIGHT).build());
            addRenderableWidget(Button.builder(Component.translatable("chestprofile.button.cancel"), button -> this.onClose())
                    .bounds(centerX + 122, bottomButtonY, ACTION_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }
    }

    private void refresh() {
        entries.clear();
        drives.clear();
        for (File root : File.listRoots()) {
            drives.add(root.toPath());
        }
        this.errorMessage = null;
        try {
            if (!Files.exists(currentDirectory)) {
                Files.createDirectories(currentDirectory);
            }
            try (Stream<Path> stream = Files.list(currentDirectory)) {
                List<Path> directories = new ArrayList<>();
                List<Path> jsonFiles = new ArrayList<>();
                stream.forEach(path -> {
                    try {
                        if (Files.isDirectory(path)) {
                            directories.add(path);
                        } else if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")) {
                            jsonFiles.add(path);
                        }
                    } catch (Exception ignored) {
                    }
                });
                Comparator<Path> nameComparator = Comparator.comparing(
                        path -> path.getFileName().toString().toLowerCase(Locale.ROOT));
                directories.sort(nameComparator);
                jsonFiles.sort(nameComparator);
                entries.addAll(directories);
                entries.addAll(jsonFiles);
            }
        } catch (IOException exception) {
            this.errorMessage = exception.toString();
            Path parentDirectory = currentDirectory.getParent();
            if (parentDirectory != null && Files.isDirectory(parentDirectory)) {
                currentDirectory = parentDirectory;
            }
        }
        this.scrollOffset = 0;
    }

    private void navigate(Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return;
        }
        currentDirectory = directory;
        refresh();
    }

    private void openSelected() {
        if (selectedPath == null) {
            return;
        }
        onOpen.accept(selectedPath);
        this.onClose();
    }

    private void savePressed() {
        String sanitizedName = ProfileConfig.sanitizeName(nameInputBox.getValue());
        onSave.accept(currentDirectory.resolve(sanitizedName + ".json"));
        this.onClose();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.returnTo);
    }

    private int visibleRows() {
        int calculatedRows = (this.height - LIST_START_Y - 44) / ROW_HEIGHT;
        return Math.max(6, Math.min(16, calculatedRows));
    }

    private int rowAt(double mouseX, double mouseY) {
        int centerX = this.width / 2;
        if (mouseX < centerX - LIST_HALF_WIDTH || mouseX >= centerX - LIST_HALF_WIDTH + LIST_WIDTH) {
            return -1;
        }
        int bottomLimit = LIST_START_Y + visibleRows() * ROW_HEIGHT;
        if (mouseY < LIST_START_Y || mouseY >= bottomLimit) {
            return -1;
        }
        return (int) ((mouseY - LIST_START_Y) / ROW_HEIGHT);
    }

    private int driveAt(double mouseX, double mouseY) {
        if (mouseY < DRIVES_START_Y || mouseY >= DRIVES_START_Y + DRIVE_HEIGHT) {
            return -1;
        }
        int driveX = this.width / 2 - LIST_HALF_WIDTH;
        for (int i = 0; i < drives.size(); i++) {
            int driveWidth = this.font.width(drives.get(i).toString()) + 10;
            if (driveX + driveWidth > this.width / 2 + LIST_HALF_WIDTH) {
                break;
            }
            if (mouseX >= driveX && mouseX < driveX + driveWidth) {
                return i;
            }
            driveX += driveWidth + 2;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int driveIndex = driveAt(event.x(), event.y());
            if (driveIndex >= 0) {
                navigate(drives.get(driveIndex));
                return true;
            }
            int rowIndex = rowAt(event.x(), event.y());
            if (rowIndex >= 0) {
                int entryIndex = scrollOffset + rowIndex;
                if (entryIndex >= 0 && entryIndex < entries.size()) {
                    Path entryPath = entries.get(entryIndex);
                    if (Files.isDirectory(entryPath)) {
                        navigate(entryPath);
                    } else if (mode == Mode.LOAD) {
                        this.selectedPath = entryPath;
                        if (doubleClick) {
                            openSelected();
                        }
                    } else {
                        this.nameInputBox.setValue(entryPath.getFileName().toString());
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScrollOffset = Math.max(0, entries.size() - visibleRows());
        int delta = scrollY > 0 ? -1 : 1;
        scrollOffset = Math.max(0, Math.min(scrollOffset + delta, maxScrollOffset));
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Font font = this.font;
        int centerX = this.width / 2;
        graphics.text(font, this.title, centerX - font.width(this.title) / 2, 8, 0xFFFFFFFF, false);

        String pathString = currentDirectory.toAbsolutePath().toString();
        String clippedPath = font.plainSubstrByWidth(pathString, this.width - 200);
        graphics.text(font, clippedPath, centerX - font.width(clippedPath) / 2, 20, 0xFFAAAAAA, false);

        int driveX = centerX - LIST_HALF_WIDTH;
        for (int i = 0; i < drives.size(); i++) {
            String label = drives.get(i).toString();
            int driveWidth = font.width(label) + 10;
            if (driveX + driveWidth > centerX + LIST_HALF_WIDTH) {
                break;
            }
            boolean isCurrentDrive = currentDirectory.startsWith(drives.get(i));
            graphics.fill(driveX, DRIVES_START_Y, driveX + driveWidth, DRIVES_START_Y + DRIVE_HEIGHT, isCurrentDrive ? 0x44FFFFFF : 0x22000000);
            graphics.horizontalLine(driveX, driveX + driveWidth, DRIVES_START_Y + DRIVE_HEIGHT - 1, 0x22FFFFFF);
            graphics.text(font, label, driveX + 5, DRIVES_START_Y + 3, isCurrentDrive ? 0xFFFFFFFF : 0xFFCCCCCC, false);
            driveX += driveWidth + 2;
        }

        if (this.errorMessage != null) {
            graphics.text(font, this.errorMessage, centerX - font.width(this.errorMessage) / 2, LIST_START_Y + 4, 0xFFFF5555, false);
            return;
        }

        int visibleCount = visibleRows();
        int listX = centerX - LIST_HALF_WIDTH;
        for (int i = 0; i < visibleCount; i++) {
            int entryIndex = scrollOffset + i;
            if (entryIndex >= entries.size()) {
                break;
            }
            Path entryPath = entries.get(entryIndex);
            boolean isDirectory = Files.isDirectory(entryPath);
            int rowY = LIST_START_Y + i * ROW_HEIGHT;
            boolean isSelected = mode == Mode.LOAD && entryPath.equals(selectedPath);

            graphics.fill(listX, rowY, listX + LIST_WIDTH, rowY + ROW_HEIGHT, isSelected ? 0x66FFFFFF : 0x22000000);
            graphics.horizontalLine(listX, listX + LIST_WIDTH, rowY + ROW_HEIGHT - 1, 0x22FFFFFF);

            String label = (isDirectory ? "[" + entryPath.getFileName() + "]/" : entryPath.getFileName().toString());
            String labelClipped = font.plainSubstrByWidth(label, LIST_WIDTH - 12);
            graphics.text(font, labelClipped, listX + 4, rowY + 5, isDirectory ? 0xFFFFFFFF : 0xFFCCCCCC, false);
        }
    }
}
