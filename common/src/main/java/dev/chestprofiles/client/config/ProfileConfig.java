package dev.chestprofiles.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Item> ITEM_ID_CACHE = new ConcurrentHashMap<>();

    public static final int MAX_CONFIGS = 50;
    public static final int MAX_PROFILES = 128;

    public static ProfileConfig instance;

    public static final Path CONFIG_FILE = getConfigDir().resolve("chestprofile.json");
    public static final Path CONFIGS_DIR = getConfigDir().resolve("chestprofile").resolve("configs");

    public static Path getConfigDir() {
        try {
            return Minecraft.getInstance().gameDirectory.toPath().resolve("config");
        } catch (Throwable ignored) {
            return Path.of("config");
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean panelEnabled = true;

    @SerializedName(value = "configs", alternate = {"profiles"})
    public List<Config> configs = new ArrayList<>();

    @SerializedName(value = "activeConfigIndex", alternate = {"activeProfileIndex"})
    public int activeConfigIndex = 0;

    public Map<String, ChestRef> chestAssignments = new HashMap<>();

    public static void load() {
        instance = new ProfileConfig();
        if (Files.exists(CONFIG_FILE)) {
            try {
                String json = Files.readString(CONFIG_FILE, StandardCharsets.UTF_8);
                ProfileConfig loaded = GSON.fromJson(json, ProfileConfig.class);
                if (loaded != null) {
                    instance = loaded;
                    if (instance.configs == null) {
                        instance.configs = new ArrayList<>();
                    }
                    if (instance.chestAssignments == null) {
                        instance.chestAssignments = new HashMap<>();
                    }
                    for (Config config : instance.configs) {
                        if (config.profiles == null) {
                            config.profiles = new ArrayList<>();
                        }
                    }
                }
            } catch (Exception exception) {
                LOGGER.error("Failed to load chestprofiles config", exception);
            }
        }
        if (instance.configs.isEmpty()) {
            instance.configs.add(new Config("Default"));
        }
        if (instance.activeConfigIndex < 0 || instance.activeConfigIndex >= instance.configs.size()) {
            instance.activeConfigIndex = 0;
        }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIGS_DIR);
            Files.writeString(CONFIG_FILE, GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.error("Failed to save chestprofiles config", exception);
        }
    }

    public static Item itemFromId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        return ITEM_ID_CACHE.computeIfAbsent(itemId, key -> {
            String normalizedKey = key.contains(":") ? key : "minecraft:" + key;
            Identifier identifier = Identifier.tryParse(normalizedKey);
            if (identifier == null || !BuiltInRegistries.ITEM.containsKey(identifier)) {
                return null;
            }
            return BuiltInRegistries.ITEM.getValue(identifier);
        });
    }

    public static String sanitizeName(String name) {
        if (name == null) {
            return "Config";
        }
        String sanitized = name.replaceAll("[\\\\/:*?\"<>|]", "").trim();
        if (sanitized.isEmpty()) {
            return "Config";
        }
        if (sanitized.length() > 24) {
            sanitized = sanitized.substring(0, 24);
        }
        return sanitized;
    }

    public Config getActiveConfig() {
        if (configs.isEmpty()) {
            return null;
        }
        if (activeConfigIndex < 0 || activeConfigIndex >= configs.size()) {
            activeConfigIndex = 0;
        }
        return configs.get(activeConfigIndex);
    }

    public void cycleConfig(boolean forward) {
        if (configs.isEmpty()) {
            return;
        }
        int count = configs.size();
        activeConfigIndex = ((activeConfigIndex + (forward ? 1 : count - 1)) % count + count) % count;
    }

    public Config addConfig(String name) {
        if (configs.size() >= MAX_CONFIGS) {
            return null;
        }
        Config config = new Config(sanitizeName(name));
        configs.add(config);
        activeConfigIndex = configs.size() - 1;
        return config;
    }

    public void deleteConfig(int index) {
        if (index < 0 || index >= configs.size()) {
            return;
        }
        configs.remove(index);
        if (configs.isEmpty()) {
            configs.add(new Config("Default"));
        }
        activeConfigIndex = Math.min(Math.max(0, index - 1), configs.size() - 1);
    }

    public void clearConfig(Config config) {
        if (config != null) {
            config.profiles.clear();
            config.activeProfile = 0;
        }
    }

    public void renameConfig(Config config, String name) {
        if (config == null) {
            return;
        }
        String sanitized = sanitizeName(name);
        if (!sanitized.isEmpty()) {
            config.name = sanitized;
        }
    }

    public void renameProfile(Config config, int profileIndex, String name) {
        Profile profile = profileOf(config, profileIndex);
        if (profile == null) {
            return;
        }
        if (name == null || name.isBlank()) {
            profile.name = null;
            return;
        }
        String sanitized = sanitizeName(name);
        if (!sanitized.isEmpty()) {
            profile.name = sanitized;
        }
    }

    public void addOrReplace(Config config) {
        if (config == null) {
            return;
        }
        for (int i = 0; i < configs.size(); i++) {
            if (configs.get(i).name.equals(config.name)) {
                configs.set(i, config);
                activeConfigIndex = i;
                return;
            }
        }
        if (configs.size() < MAX_CONFIGS) {
            configs.add(config);
            activeConfigIndex = configs.size() - 1;
        }
    }

    public static Profile profileOf(Config config, int profileIndex) {
        if (config == null || profileIndex < 0 || profileIndex >= config.profiles.size()) {
            return null;
        }
        Profile profile = config.profiles.get(profileIndex);
        return profile != null && !profile.items.isEmpty() ? profile : null;
    }

    public static Profile activeProfileOf(Config config) {
        return config == null ? null : profileOf(config, config.activeProfile);
    }

    public static int profileSlots(Profile profile) {
        if (profile == null) {
            return 0;
        }
        int total = 0;
        for (ItemEntry entry : profile.items) {
            if (entry != null) {
                total += Math.max(1, entry.count);
            }
        }
        return total;
    }

    public static List<ItemEntry> scaledLayout(Profile profile, int[] requiredStacks, int capacity) {
        List<ItemEntry> entries = new ArrayList<>();
        List<Integer> allocatedStacks = new ArrayList<>();
        for (int i = 0; i < profile.items.size(); i++) {
            ItemEntry entry = profile.items.get(i);
            if (entry == null || entry.item == null || entry.item.isBlank()
                    || itemFromId(entry.item) == null) {
                continue;
            }
            entries.add(entry);
            allocatedStacks.add(Math.max(0, requiredStacks[i]));
        }

        List<ItemEntry> layout = new ArrayList<>();
        for (int i = 0; i < entries.size() && layout.size() < capacity; i++) {
            for (int k = 0; k < allocatedStacks.get(i) && layout.size() < capacity; k++) {
                layout.add(entries.get(i));
            }
        }
        return layout;
    }

    public static int[] stacksPerEntry(Profile profile, Inventory playerInventory, List<Slot> chestSlots) {
        Map<Item, Integer> playerCounts = new HashMap<>();
        for (ItemStack stack : playerInventory.getNonEquipmentItems()) {
            if (!stack.isEmpty()) {
                playerCounts.merge(stack.getItem(), 1, Integer::sum);
            }
        }
        Map<Item, Integer> chestCounts = new HashMap<>();
        for (Slot slot : chestSlots) {
            if (slot.hasItem()) {
                chestCounts.merge(slot.getItem().getItem(), 1, Integer::sum);
            }
        }
        int[] stacks = new int[profile.items.size()];
        for (int i = 0; i < profile.items.size(); i++) {
            ItemEntry entry = profile.items.get(i);
            if (entry == null) {
                continue;
            }
            Item item = itemFromId(entry.item);
            if (item == null) {
                continue;
            }
            stacks[i] = Math.max(1, playerCounts.getOrDefault(item, 0) + chestCounts.getOrDefault(item, 0));
        }
        return stacks;
    }

    public static boolean profileContains(Profile profile, ItemStack stack) {
        if (profile == null || stack.isEmpty()) {
            return false;
        }
        Item targetItem = stack.getItem();
        for (ItemEntry entry : profile.items) {
            Item item = itemFromId(entry.item);
            if (item != null && targetItem == item) {
                return true;
            }
        }
        return false;
    }

    public static String chestKey(String dimension, int x, int y, int z) {
        return dimension + ":" + x + "," + y + "," + z;
    }

    public void applyChestAssignment(String key) {
        if (key == null) {
            return;
        }
        ChestRef assignment = chestAssignments.get(key);
        if (assignment == null) {
            Config config = getActiveConfig();
            if (config != null) {
                config.activeProfile = -1;
            }
            return;
        }
        if (assignment.config >= 0 && assignment.config < configs.size()) {
            activeConfigIndex = assignment.config;
        }
        Config config = getActiveConfig();
        if (config != null) {
            config.activeProfile = assignment.profile >= 0 && profileOf(config, assignment.profile) != null ? assignment.profile : -1;
        }
    }

    public void selectChestProfile(String key, int configIndex, int profileIndex) {
        if (configIndex >= 0 && configIndex < configs.size()) {
            activeConfigIndex = configIndex;
        }
        Config config = getActiveConfig();
        if (config != null) {
            config.activeProfile = profileIndex;
        }
        if (key != null) {
            chestAssignments.put(key, new ChestRef(activeConfigIndex, profileIndex));
        }
        save();
    }

    public Config importFromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            LayoutImport layout = GSON.fromJson(json, LayoutImport.class);
            if (layout != null && layout.items != null && !layout.items.isEmpty()) {
                return fromLayout(layout);
            }
        } catch (Exception ignored) {
        }
        try {
            FlatItem[] flat = GSON.fromJson(json, FlatItem[].class);
            if (flat != null && flat.length > 0) {
                return fromFlat(flat, "Imported");
            }
        } catch (Exception ignored) {
        }
        try {
            WrapperImport wrapper = GSON.fromJson(json, WrapperImport.class);
            if (wrapper != null) {
                List<FlatItem> allItems = new ArrayList<>();
                if (wrapper.items != null) {
                    allItems.addAll(wrapper.items);
                }
                if (wrapper.profiles != null) {
                    allItems.addAll(wrapper.profiles);
                }
                if (wrapper.cells != null) {
                    allItems.addAll(wrapper.cells);
                }
                if (!allItems.isEmpty()) {
                    return fromFlat(allItems.toArray(new FlatItem[0]), "Imported");
                }
            }
        } catch (Exception ignored) {
        }
        try {
            Config config = GSON.fromJson(json, Config.class);
            if (config != null && config.profiles != null && !config.profiles.isEmpty()) {
                return finalizeConfig(config);
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to parse config JSON", exception);
        }
        return null;
    }

    private Config fromLayout(LayoutImport layout) {
        Config config = new Config(layout.name == null || layout.name.isBlank() ? "Imported" : layout.name);
        applyFlatItems(config, layout.items);
        return config.profiles.isEmpty() ? null : config;
    }

    private Config fromFlat(FlatItem[] flatItems, String name) {
        Config config = new Config(name);
        applyFlatItems(config, Arrays.asList(flatItems));
        return config.profiles.isEmpty() ? null : config;
    }

    private void applyFlatItems(Config config, List<FlatItem> flatItems) {
        config.profiles = new ArrayList<>();
        TreeMap<Integer, List<ItemEntry>> itemsByProfile = new TreeMap<>();
        for (FlatItem flatItem : flatItems) {
            if (flatItem == null || flatItem.item == null || flatItem.item.isBlank()) {
                continue;
            }
            if (itemFromId(flatItem.item) == null) {
                continue;
            }
            int profileIndex = flatItem.profileIndex();
            if (profileIndex < 0) {
                profileIndex = 0;
            }
            if (profileIndex >= MAX_PROFILES) {
                continue;
            }
            itemsByProfile.computeIfAbsent(profileIndex, k -> new ArrayList<>())
                    .add(new ItemEntry(flatItem.item, Math.max(1, flatItem.count)));
        }
        for (Map.Entry<Integer, List<ItemEntry>> entry : itemsByProfile.entrySet()) {
            int profileIndex = entry.getKey();
            while (config.profiles.size() < profileIndex) {
                config.profiles.add(new Profile());
            }
            Profile profile = new Profile();
            profile.items = entry.getValue();
            config.profiles.add(profile);
        }
        config.activeProfile = -1;
    }

    private Config finalizeConfig(Config config) {
        if (config == null) {
            return null;
        }
        config.name = sanitizeName(config.name == null ? "Imported" : config.name);
        if (config.profiles == null) {
            config.profiles = new ArrayList<>();
        }
        List<Profile> validProfiles = new ArrayList<>();
        for (Profile profile : config.profiles) {
            if (profile == null) {
                validProfiles.add(new Profile());
                continue;
            }
            List<ItemEntry> validEntries = new ArrayList<>();
            for (ItemEntry entry : profile.items) {
                if (entry == null || entry.item == null || entry.item.isBlank()
                        || itemFromId(entry.item) == null) {
                    continue;
                }
                if (entry.count < 1) {
                    entry.count = 1;
                }
                validEntries.add(entry);
            }
            profile.items = validEntries;
            validProfiles.add(profile);
        }
        config.profiles = validProfiles;
        if (config.activeProfile < -1 || config.activeProfile >= config.profiles.size()) {
            config.activeProfile = -1;
        }
        return config.profiles.isEmpty() ? null : config;
    }

    public String exportToJson(Config config) {
        LayoutImport exported = new LayoutImport();
        exported.name = config.name;
        exported.items = new ArrayList<>();
        for (int i = 0; i < config.profiles.size(); i++) {
            Profile profile = config.profiles.get(i);
            if (profile == null) {
                continue;
            }
            for (ItemEntry entry : profile.items) {
                exported.items.add(new FlatItem(entry.item, entry.count, i));
            }
        }
        return GSON.toJson(exported);
    }

    public Config importFromClipboard() {
        return importFromJson(Minecraft.getInstance().keyboardHandler.getClipboard());
    }

    public void exportToClipboard(Config config) {
        if (config != null) {
            Minecraft.getInstance().keyboardHandler.setClipboard(exportToJson(config));
        }
    }

    public Config importFromFile(File file) {
        if (file == null) {
            return null;
        }
        try {
            return importFromJson(Files.readString(file.toPath(), StandardCharsets.UTF_8));
        } catch (Exception exception) {
            LOGGER.warn("Failed to import config from {}", file, exception);
            return null;
        }
    }

    public boolean exportToFile(Config config, File file) {
        if (config == null || file == null) {
            return false;
        }
        try {
            Path path = file.toPath();
            if (!file.getName().endsWith(".json")) {
                path = file.toPath().resolveSibling(file.getName() + ".json");
            }
            Files.writeString(path, exportToJson(config), StandardCharsets.UTF_8);
            return true;
        } catch (Exception exception) {
            LOGGER.warn("Failed to export config to {}", file, exception);
            return false;
        }
    }

    public static class Config {
        public String name;
        @SerializedName(value = "activeProfile", alternate = {"activeCell"})
        public int activeProfile = -1;
        @SerializedName(value = "profiles", alternate = {"cells"})
        public List<Profile> profiles = new ArrayList<>();

        public Config() {
        }

        public Config(String name) {
            this.name = sanitizeName(name);
        }
    }

    public static class Profile {
        public String name;
        public List<ItemEntry> items = new ArrayList<>();

        public Profile() {
        }
    }

    public static class ItemEntry {
        @SerializedName("id")
        public String item;
        @SerializedName("count")
        public int count = 1;

        public ItemEntry() {
        }

        public ItemEntry(String item, int count) {
            this.item = item;
            this.count = Math.max(1, count);
        }
    }

    public static class ChestRef {
        @SerializedName(value = "config", alternate = {"profile"})
        public int config;
        @SerializedName(value = "profile", alternate = {"cell"})
        public int profile;

        public ChestRef() {
        }

        public ChestRef(int config, int profile) {
            this.config = config;
            this.profile = profile;
        }
    }

    private static class LayoutImport {
        public String name;
        public List<FlatItem> items;
    }

    private static class FlatItem {
        @SerializedName("id")
        public String item;
        @SerializedName("count")
        public int count = 1;
        @SerializedName(value = "profile", alternate = {"cell"})
        public int profile;

        public FlatItem() {
        }

        public FlatItem(String item, int count, int profile) {
            this.item = item;
            this.count = Math.max(1, count);
            this.profile = profile;
        }

        public int profileIndex() {
            return profile;
        }
    }

    private static class WrapperImport {
        public List<FlatItem> items;
        public List<FlatItem> profiles;
        public List<FlatItem> cells;
    }
}
