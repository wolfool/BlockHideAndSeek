package com.github.pmh75.blockhideandseek;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Mode2BlockManageMenu implements Listener {

    private static final String TITLE = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "모드2 선택 블럭 관리";

    private final BlockHideAndSeek plugin;
    private final Set<UUID> openMenus = new HashSet<>();
    private final Map<UUID, List<ConfigEntry>> shownEntries = new HashMap<>();

    public Mode2BlockManageMenu(BlockHideAndSeek plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        List<ConfigEntry> entries = loadEntries();
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        List<ConfigEntry> shown = new ArrayList<>();

        int limit = Math.min(entries.size(), inv.getSize());
        for (int i = 0; i < limit; i++) {
            ConfigEntry entry = entries.get(i);
            shown.add(entry);
            inv.setItem(i, createEntryItem(player, entry));
        }

        UUID uuid = player.getUniqueId();
        openMenus.add(uuid);
        shownEntries.put(uuid, shown);
        player.openInventory(inv);
        player.sendMessage(ChatColor.GRAY + "아래 인벤토리 아이템 좌/우클릭 = 추가, 위 목록 좌/우클릭 = 제거");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openMenus.contains(player.getUniqueId())) return;
        if (!TITLE.equals(event.getView().getTitle())) return;

        event.setCancelled(true);
        if (!isLeftOrRightClick(event.getClick())) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0) return;

        if (rawSlot < topSize) {
            handleTopClick(player, rawSlot, event.getCursor());
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        addFromItem(player, clicked);
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!TITLE.equals(event.getView().getTitle())) return;
        UUID uuid = player.getUniqueId();
        openMenus.remove(uuid);
        shownEntries.remove(uuid);
    }
    private void handleTopClick(Player player, int rawSlot, ItemStack cursor) {
        List<ConfigEntry> entries = shownEntries.get(player.getUniqueId());
        if (entries != null && rawSlot < entries.size()) {
            ConfigEntry entry = entries.get(rawSlot);
            if (removeEntry(entry)) {
                player.sendMessage(ChatColor.GREEN + entry.id + " 제거됨");
            }
            refresh(player);
            return;
        }

        if (cursor != null && cursor.getType() != Material.AIR) {
            addFromItem(player, cursor);
        }
    }

    private void addFromItem(Player player, ItemStack item) {
        CraftEngineHook hook = plugin.getCraftEngineHook();
        String customId = hook == null ? null : hook.getCustomItemId(item);
        if (customId != null) {
            addEntryAndMessage(player, new ConfigEntry("craftengine.blocks", customId, true));
            return;
        }

        Material mat = item.getType();
        if (!mat.isBlock()) {
            player.sendMessage(ChatColor.RED + "선택 가능 목록에는 블럭 아이템만 넣을 수 있습니다.");
            return;
        }

        addEntryAndMessage(player, new ConfigEntry("selectable-blocks", mat.name(), false));
    }

    private void addEntryAndMessage(Player player, ConfigEntry entry) {
        boolean added = addEntry(entry);
        player.sendMessage(added
                ? ChatColor.GREEN + entry.id + " 추가됨"
                : ChatColor.YELLOW + entry.id + " 은(는) 이미 목록에 있습니다.");
        refresh(player);
    }


    private void refresh(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> open(player));
    }
    private ItemStack createEntryItem(Player player, ConfigEntry entry) {
        ItemStack item;
        if (entry.custom) {
            CraftEngineHook hook = plugin.getCraftEngineHook();
            DisguiseBlock block = hook == null ? null : hook.createBlock(entry.id, player);
            item = block == null ? new ItemStack(Material.BARRIER) : block.createIcon();
        } else {
            Material mat = Material.matchMaterial(entry.id);
            item = new ItemStack(mat != null && mat.isItem() ? mat : Material.BARRIER);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            ChatColor color = entry.custom ? ChatColor.AQUA : ChatColor.GREEN;
            meta.setDisplayName(color + entry.id);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "좌클/우클릭하면 목록에서 제거됩니다.",
                    ChatColor.DARK_GRAY + (entry.custom ? "CraftEngine" : "Vanilla")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isLeftOrRightClick(ClickType click) {
        return click == ClickType.LEFT || click == ClickType.RIGHT;
    }

    private List<ConfigEntry> loadEntries() {
        List<ConfigEntry> entries = new ArrayList<>();
        for (String id : plugin.getConfig().getStringList("selectable-blocks")) {
            if (id != null && !id.isBlank()) {
                entries.add(new ConfigEntry("selectable-blocks", id.trim().toUpperCase(Locale.ROOT), false));
            }
        }
        for (String id : plugin.getConfig().getStringList("craftengine.blocks")) {
            if (id != null && !id.isBlank()) {
                entries.add(new ConfigEntry("craftengine.blocks", id.trim(), true));
            }
        }
        return entries;
    }

    private boolean addEntry(ConfigEntry entry) {
        List<String> list = new ArrayList<>(plugin.getConfig().getStringList(entry.path));
        if (containsIgnoreCase(list, entry.id)) {
            return false;
        }
        list.add(entry.id);
        plugin.getConfig().set(entry.path, list);
        plugin.saveConfig();
        return true;
    }

    private boolean removeEntry(ConfigEntry entry) {
        List<String> list = new ArrayList<>(plugin.getConfig().getStringList(entry.path));
        boolean removed = list.removeIf(value -> value.equalsIgnoreCase(entry.id));
        if (removed) {
            plugin.getConfig().set(entry.path, list);
            plugin.saveConfig();
        }
        return removed;
    }

    private boolean containsIgnoreCase(List<String> values, String target) {
        for (String value : values) {
            if (value.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    private record ConfigEntry(String path, String id, boolean custom) { }
}
