package com.playgamesinteractive.lushvotes.bridge.menu;

import com.playgamesinteractive.lushvotes.bridge.action.Action;
import com.playgamesinteractive.lushvotes.bridge.action.ActionParser;
import com.playgamesinteractive.lushvotes.bridge.lang.ColorText;
import com.playgamesinteractive.lushvotes.bridge.util.ItemCosmetics;
import com.playgamesinteractive.lushvotes.bridge.util.Placeholders;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Loads menus/*.yml from the plugin data folder and builds Bukkit
 * inventories from them on demand. Menu layout/text/icons are fully
 * config-driven; each item's actions run through the shared ActionRunner.
 * Only bundled default here is {@code vote_menu.yml}. Icons are built fresh
 * on every {@link #open} (not cached at load time) so display name/lore can
 * carry live PlaceholderAPI values (e.g. %lushvotes_total_formatted%) per
 * viewer - decoration layers (fill/corner/accent) are the exception, built
 * once at load time since they never carry placeholders.
 */
public class MenuManager {

    private static final String[] BUNDLED_DEFAULTS = {
            "vote_menu.yml"
    };

    private final Plugin plugin;
    private final File menusDir;
    // volatile immutable snapshot: reload happens on one region thread while
    // opens read from many on Folia
    private volatile Map<String, Menu> menus = Map.of();

    public MenuManager(Plugin plugin) {
        this.plugin = plugin;
        this.menusDir = new File(plugin.getDataFolder(), "menus");
    }

    public void loadAll() {
        if (!menusDir.exists()) {
            menusDir.mkdirs();
        }
        extractBundledDefaultsIfMissing();

        File[] files = menusDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            menus = Map.of();
            return;
        }
        Map<String, Menu> loaded = new LinkedHashMap<>();
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 4);
            try {
                loaded.put(id, loadMenuFile(id, file));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load menu file " + file.getName(), e);
            }
        }
        menus = Map.copyOf(loaded);
        plugin.getLogger().info("Loaded " + loaded.size() + " menu(s).");
    }

    private void extractBundledDefaultsIfMissing() {
        for (String fileName : BUNDLED_DEFAULTS) {
            if (!new File(menusDir, fileName).exists() && plugin.getResource("menus/" + fileName) != null) {
                plugin.saveResource("menus/" + fileName, false);
            }
        }
    }

    private Menu loadMenuFile(String id, File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String title = yaml.getString("title", id);
        int size = yaml.getInt("size", 27);
        if (size < 9 || size > 54 || size % 9 != 0) {
            plugin.getLogger().warning("Invalid size " + size + " in " + file.getName() + " - must be a multiple of 9 between 9 and 54. Using 27.");
            size = 27;
        }

        ItemStack fill = parseDecoration(yaml, "fill.", file.getName());
        ItemStack corner = parseDecoration(yaml, "corner.", file.getName());
        ItemStack accent = parseDecoration(yaml, "accent.", file.getName());
        List<Integer> accentSlots = accent == null ? List.of()
                : inBounds(parseSlots(yaml.getList("accent.accent-slots", List.of())), size, "accent-slots", file.getName());

        Map<Integer, MenuItem> items = new LinkedHashMap<>();
        var itemsSection = yaml.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                String path = "items." + key + ".";
                String materialName = yaml.getString(path + "material", "STONE");
                Material material = Material.matchMaterial(materialName);
                if (material == null) {
                    plugin.getLogger().warning("Unknown material '" + materialName + "' in " + file.getName());
                    material = Material.STONE;
                }
                List<Integer> slots = resolveItemSlots(yaml, path, key);
                if (slots.isEmpty()) {
                    plugin.getLogger().warning("Skipping item '" + key + "' in " + file.getName()
                            + " - no valid slot/slots given and key isn't numeric.");
                    continue;
                }
                String displayName = yaml.getString(path + "display_name", "");
                List<String> lore = yaml.getStringList(path + "lore");
                List<Action> actions = ActionParser.parse(yaml.getStringList(path + "actions"),
                        file.getName() + " item '" + key + "'", plugin.getLogger());
                String permission = yaml.getString(path + "permission", null);
                List<ItemFlag> itemFlags = ItemCosmetics.parseItemFlags(yaml.getStringList(path + "item_flags"), file.getName(), plugin.getLogger());
                Boolean glintOverride = yaml.isSet(path + "enchantment_glint_override")
                        ? yaml.getBoolean(path + "enchantment_glint_override") : null;
                boolean hideTooltip = yaml.getBoolean(path + "hide_tooltip", false);

                for (int slot : inBounds(slots, size, "item '" + key + "'", file.getName())) {
                    items.put(slot, new MenuItem(slot, material, displayName, lore, itemFlags, glintOverride, hideTooltip, actions, permission));
                }
            }
        }

        return new Menu(id, title, size, fill, corner, accent, accentSlots, items);
    }

    /** {@code fill.material}/{@code corner.material}/{@code accent.material} - absent entirely means "skip this layer". */
    private ItemStack parseDecoration(YamlConfiguration yaml, String path, String fileName) {
        if (!yaml.isSet(path + "material")) {
            return null;
        }
        String materialName = yaml.getString(path + "material");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            plugin.getLogger().warning("Unknown material '" + materialName + "' in " + fileName);
            material = Material.STONE;
        }
        String displayName = yaml.getString(path + "display_name", " ");
        List<ItemFlag> itemFlags = ItemCosmetics.parseItemFlags(yaml.getStringList(path + "item_flags"), fileName, plugin.getLogger());
        Boolean glintOverride = yaml.isSet(path + "enchantment_glint_override")
                ? yaml.getBoolean(path + "enchantment_glint_override") : null;
        boolean hideTooltip = yaml.getBoolean(path + "hide_tooltip", false);
        return buildIcon(material, displayName, List.of(), itemFlags, glintOverride, hideTooltip);
    }

    /** slot/slots can be given explicitly; otherwise the item's own YAML key is tried as a single numeric slot. */
    private List<Integer> resolveItemSlots(YamlConfiguration yaml, String path, String key) {
        List<?> rawSlots = yaml.getList(path + "slots");
        if (rawSlots != null && !rawSlots.isEmpty()) {
            return parseSlots(rawSlots);
        }
        if (yaml.isSet(path + "slot")) {
            Integer single = tryParseInt(String.valueOf(yaml.get(path + "slot")));
            return single != null ? List.of(single) : List.of();
        }
        Integer keyAsSlot = tryParseInt(key);
        return keyAsSlot != null ? List.of(keyAsSlot) : List.of();
    }

    /**
     * Expands a raw YAML list into concrete slot numbers - a filler like
     * "slots: [1, 9, 17, 25]" or a range "start-end" (both ends inclusive,
     * order-independent), mixed freely. Unparseable entries are logged and
     * skipped rather than failing the whole menu.
     */
    private List<Integer> parseSlots(List<?> raw) {
        List<Integer> slots = new ArrayList<>();
        for (Object entry : raw) {
            if (entry instanceof Number number) {
                slots.add(number.intValue());
                continue;
            }
            String text = String.valueOf(entry).trim();
            if (text.matches("\\d+-\\d+")) {
                String[] parts = text.split("-", 2);
                int start = Integer.parseInt(parts[0]);
                int end = Integer.parseInt(parts[1]);
                for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
                    slots.add(i);
                }
                continue;
            }
            Integer parsed = tryParseInt(text);
            if (parsed != null) {
                slots.add(parsed);
            } else {
                plugin.getLogger().warning("Invalid slot entry '" + text + "'");
            }
        }
        return slots;
    }

    /** Drops any slot outside [0, size) instead of letting it crash Inventory#setItem later. */
    private List<Integer> inBounds(List<Integer> slots, int size, String context, String fileName) {
        List<Integer> valid = new ArrayList<>();
        for (int slot : slots) {
            if (slot < 0 || slot >= size) {
                plugin.getLogger().warning("Slot " + slot + " for " + context + " in " + fileName
                        + " is out of bounds for a size-" + size + " menu (valid range 0-" + (size - 1) + ") - skipping.");
            } else {
                valid.add(slot);
            }
        }
        return valid;
    }

    private Integer tryParseInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void open(Player viewer, String menuId) {
        Menu menu = menus.get(menuId);
        if (menu == null) {
            viewer.sendMessage(ColorText.of("<red>That menu isn't configured."));
            return;
        }

        MenuHolder holder = new MenuHolder(menu);
        Component title = ColorText.of(menu.title());
        Inventory inventory = Bukkit.createInventory(holder, menu.size(), title);
        holder.setInventory(inventory);

        paintDecorations(inventory, menu);

        for (MenuItem item : menu.itemList()) {
            if (item.permission() != null && !viewer.hasPermission(item.permission())) {
                continue;
            }
            inventory.setItem(item.slot(), buildViewerIcon(item, viewer));
        }

        viewer.openInventory(inventory);
    }

    /** fill (every slot) -> corner (0 and size-1) -> accent (accent-slots) - each layer overwrites the previous where they overlap. */
    private void paintDecorations(Inventory inventory, Menu menu) {
        if (menu.fill() != null) {
            for (int slot = 0; slot < menu.size(); slot++) {
                inventory.setItem(slot, menu.fill().clone());
            }
        }
        if (menu.corner() != null) {
            inventory.setItem(0, menu.corner().clone());
            inventory.setItem(menu.size() - 1, menu.corner().clone());
        }
        if (menu.accent() != null) {
            for (int slot : menu.accentSlots()) {
                inventory.setItem(slot, menu.accent().clone());
            }
        }
    }

    private ItemStack buildViewerIcon(MenuItem item, Player viewer) {
        String displayName = Placeholders.apply(viewer, item.displayName());
        List<String> lore = item.lore().stream().map(line -> Placeholders.apply(viewer, line)).toList();
        return buildIcon(item.material(), displayName, lore, item.itemFlags(), item.enchantmentGlintOverride(), item.hideTooltip());
    }

    private ItemStack buildIcon(Material material, String displayName, List<String> lore,
                                 List<ItemFlag> itemFlags, Boolean glintOverride, boolean hideTooltip) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            ItemCosmetics.decorate(meta, displayName, lore, itemFlags, glintOverride, hideTooltip);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
