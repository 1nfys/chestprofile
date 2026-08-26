package dev.chestprofiles.client.gui;

import dev.chestprofiles.client.FpsCap;
import dev.chestprofiles.client.config.ProfileConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class SettingsScreen extends Screen {

    private static final String LAYOUT_BUILDER_URL = "https://mis-builder.cubicmetre.net/";

    private static final int PANEL_WIDTH = 200;
    private static final int HALF_PANEL_WIDTH = 100;
    private static final int ROW_START_Y = 34;
    private static final int ROW_STEP_Y = 24;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH_TRIPLE = 64;
    private static final int BUTTON_WIDTH_DOUBLE = 98;
    private static final int BUTTON_GAP = 4;
    private static final int ARROW_BUTTON_WIDTH = 16;
    private static final int EDIT_BOX_WIDTH = 160;

    private final AbstractContainerScreen<?> parent;
    private EditBox profileNameBox;
    private int selectedProfile = -1;
    private int pickerY;
    private Component status;
    private boolean statusError;

    public SettingsScreen(AbstractContainerScreen<?> parent) {
        super(Component.translatable("chestprofile.screen.settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        FpsCap.begin();
        ProfileConfig profileConfig = ProfileConfig.instance;
        int rowY = ROW_START_Y;
        this.pickerY = rowY;

        ProfileConfig.Config activeConfig = profileConfig.getActiveConfig();
        selectedProfile = activeConfig != null && !activeConfig.profiles.isEmpty() ? Math.max(0, activeConfig.activeProfile) : -1;

        int panelLeft = this.width / 2 - HALF_PANEL_WIDTH;
        int rightColumnLeft = panelLeft + BUTTON_WIDTH_DOUBLE + BUTTON_GAP;

        addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleProfile(-1))
                .bounds(panelLeft, rowY, ARROW_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        this.profileNameBox = addRenderableWidget(new EditBox(this.font, panelLeft + ARROW_BUTTON_WIDTH + BUTTON_GAP, rowY, EDIT_BOX_WIDTH, BUTTON_HEIGHT,
                Component.translatable("chestprofile.screen.profileName")));
        this.profileNameBox.setMaxLength(24);
        this.profileNameBox.setValue(profileName());
        this.profileNameBox.setFocused(false);
        this.profileNameBox.setResponder(text -> {
            ProfileConfig.Config currentConfig = profileConfig.getActiveConfig();
            if (currentConfig != null && selectedProfile >= 0) {
                profileConfig.renameProfile(currentConfig, selectedProfile, text);
                ProfileConfig.save();
            }
        });
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleProfile(1))
                .bounds(panelLeft + ARROW_BUTTON_WIDTH + BUTTON_GAP + EDIT_BOX_WIDTH + BUTTON_GAP, rowY, ARROW_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        rowY += ROW_STEP_Y;
        refreshProfilePicker();

        addToggle(rowY, "chestprofile.option.panelEnabled",
                () -> profileConfig.panelEnabled, value -> profileConfig.panelEnabled = value);
        rowY += ROW_STEP_Y;

        addRenderableWidget(Button.builder(Component.translatable("chestprofile.button.new"), button -> {
            ProfileConfig.Config newConfig = profileConfig.addConfig("Config");
            if (newConfig != null) {
                ProfileConfig.save();
                refreshProfilePicker();
                setStatus(Component.translatable("chestprofile.status.created", newConfig.name), false);
            } else {
                setStatus(Component.translatable("chestprofile.status.tooMany"), true);
            }
        }).bounds(panelLeft, rowY, BUTTON_WIDTH_TRIPLE, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("chestprofile.button.delete"), button -> {
            profileConfig.deleteConfig(profileConfig.activeConfigIndex);
            ProfileConfig.save();
            refreshProfilePicker();
            setStatus(Component.translatable("chestprofile.status.deleted"), false);
        }).bounds(panelLeft + BUTTON_WIDTH_TRIPLE + BUTTON_GAP, rowY, BUTTON_WIDTH_TRIPLE, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("chestprofile.button.migrate"), button -> {
            int migrated = profileConfig.migrateDefaultChestsToCurrentScope();
            if (migrated > 0) {
                setStatus(Component.translatable("chestprofile.status.migrated", migrated), false);
            } else {
                setStatus(Component.translatable("chestprofile.status.noMigrationData"), false);
            }
        })
        .tooltip(Tooltip.create(Component.translatable("chestprofile.tooltip.migrate")))
        .bounds(panelLeft + (BUTTON_WIDTH_TRIPLE + BUTTON_GAP) * 2, rowY, BUTTON_WIDTH_TRIPLE, BUTTON_HEIGHT).build());
        rowY += ROW_STEP_Y;

        addRenderableWidget(Button.builder(Component.translatable("chestprofile.button.import"), button -> {
            ProfileConfig.Config importedConfig = profileConfig.importFromClipboard();
            if (importedConfig != null) {
                profileConfig.addOrReplace(importedConfig);
                ProfileConfig.save();
                setStatus(Component.translatable("chestprofile.status.imported", importedConfig.name), false);
            } else {
                setStatus(Component.translatable("chestprofile.status.importFailed"), true);
            }
        }).bounds(panelLeft, rowY, BUTTON_WIDTH_DOUBLE, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("chestprofile.button.export"), button -> {
            ProfileConfig.Config currentConfig = profileConfig.getActiveConfig();
            if (currentConfig != null) {
                profileConfig.exportToClipboard(currentConfig);
                setStatus(Component.translatable("chestprofile.status.exported", currentConfig.name), false);
            }
        }).bounds(rightColumnLeft, rowY, BUTTON_WIDTH_DOUBLE, BUTTON_HEIGHT).build());
        rowY += ROW_STEP_Y;

        addRenderableWidget(Button.builder(Component.translatable("chestprofile.button.chooseFile"), button ->
                Minecraft.getInstance().gui.setScreen(new FilePickerScreen(FilePickerScreen.Mode.LOAD, null,
                        filePath -> {
                            ProfileConfig.Config imported = profileConfig.importFromFile(filePath.toFile());
                            if (imported != null) {
                                String fileStem = filePath.getFileName().toString().replaceAll("(?i)\\.json$", "");
                                imported.name = ProfileConfig.sanitizeName(fileStem);
                                profileConfig.addOrReplace(imported);
                                ProfileConfig.save();
                                setStatus(Component.translatable("chestprofile.status.importedFile", imported.name), false);
                            } else {
                                setStatus(Component.translatable("chestprofile.status.fileFailed"), true);
                            }
                        }, null, SettingsScreen.this))
        ).bounds(panelLeft, rowY, BUTTON_WIDTH_DOUBLE, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("chestprofile.button.saveAs"), button -> {
            ProfileConfig.Config currentConfig = profileConfig.getActiveConfig();
            if (currentConfig == null) {
                return;
            }
            Minecraft.getInstance().gui.setScreen(new FilePickerScreen(FilePickerScreen.Mode.SAVE,
                    ProfileConfig.sanitizeName(currentConfig.name) + ".json",
                    null,
                    filePath -> {
                        if (profileConfig.exportToFile(currentConfig, filePath.toFile())) {
                            setStatus(Component.translatable("chestprofile.status.exportedFile", currentConfig.name), false);
                        } else {
                            setStatus(Component.translatable("chestprofile.status.fileFailed"), true);
                        }
                    }, SettingsScreen.this));
        }).bounds(rightColumnLeft, rowY, BUTTON_WIDTH_DOUBLE, BUTTON_HEIGHT).build());
        rowY += ROW_STEP_Y;

        addRenderableWidget(Button.builder(Component.translatable("chestprofile.button.layout"), button ->
                Util.getPlatform().openUri(LAYOUT_BUILDER_URL)).bounds(panelLeft, rowY, BUTTON_WIDTH_DOUBLE, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("chestprofile.button.done"), button -> this.onClose())
                .bounds(rightColumnLeft, rowY, BUTTON_WIDTH_DOUBLE, BUTTON_HEIGHT).build());
    }

    private void addToggle(int rowY, String key, BooleanSupplier getter, Consumer<Boolean> setter) {
        Button button = Button.builder(toggleLabel(key, getter.getAsBoolean()), btn -> {
            setter.accept(!getter.getAsBoolean());
            btn.setMessage(toggleLabel(key, getter.getAsBoolean()));
            ProfileConfig.save();
        }).bounds(this.width / 2 - HALF_PANEL_WIDTH, rowY, PANEL_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(button);
    }

    private static Component toggleLabel(String key, boolean value) {
        return Component.translatable(key)
                .append(": ")
                .append(Component.translatable(value ? "chestprofile.value.on" : "chestprofile.value.off"));
    }

    private void cycleProfile(int delta) {
        ProfileConfig profileConfig = ProfileConfig.instance;
        ProfileConfig.Config currentConfig = profileConfig.getActiveConfig();
        if (currentConfig == null || currentConfig.profiles.isEmpty()) {
            return;
        }
        if (selectedProfile < 0) {
            selectedProfile = 0;
        }
        selectedProfile = (selectedProfile + delta + currentConfig.profiles.size()) % currentConfig.profiles.size();
        currentConfig.activeProfile = selectedProfile;
        ProfileConfig.save();
        profileNameBox.setValue(profileName());
    }

    private String profileName() {
        ProfileConfig.Config currentConfig = ProfileConfig.instance.getActiveConfig();
        ProfileConfig.Profile profile = ProfileConfig.profileOf(currentConfig, selectedProfile);
        return profile != null && profile.name != null ? profile.name : "";
    }

    private void refreshProfilePicker() {
        ProfileConfig.Config currentConfig = ProfileConfig.instance.getActiveConfig();
        selectedProfile = currentConfig != null && !currentConfig.profiles.isEmpty() ? Math.max(0, currentConfig.activeProfile) : -1;
        profileNameBox.setValue(profileName());
    }

    private void setStatus(Component message, boolean isError) {
        this.status = message;
        this.statusError = isError;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(this.font, this.title,
                this.width / 2 - this.font.width(this.title) / 2, 12, 0xFFFFFFFF, false);

        ProfileConfig.Config currentConfig = ProfileConfig.instance.getActiveConfig();
        int profileCount = currentConfig == null ? 0 : currentConfig.profiles.size();
        Component profileLabel;
        if (profileCount <= 0) {
            profileLabel = Component.translatable("chestprofile.profile.none");
        } else {
            int profileIndex = selectedProfile >= 0 ? selectedProfile : Math.max(0, currentConfig.activeProfile);
            profileLabel = Component.translatable("chestprofile.profile", profileIndex + 1, profileCount);
            ProfileConfig.Profile profile = ProfileConfig.profileOf(currentConfig, profileIndex);
            if (profile != null && profile.name != null && !profile.name.isEmpty()) {
                profileLabel = Component.translatable("chestprofile.profileNamed", profileIndex + 1, profileCount, profile.name);
            }
        }
        String clippedLabel = this.font.plainSubstrByWidth(profileLabel.getString(), this.width - 40);
        graphics.text(this.font, clippedLabel,
                this.width / 2 - this.font.width(clippedLabel) / 2, this.pickerY - 12, 0xFFFFFFFF, false);

        if (this.status != null) {
            graphics.text(this.font, this.status,
                    this.width / 2 - this.font.width(this.status) / 2, this.height - 22,
                    this.statusError ? 0xFFFF5555 : 0xFFFFFFFF, false);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

    @Override
    public void removed() {
        FpsCap.end();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
