package com.github.pmh75.blockhideandseek;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class BlockSelectMenu implements Listener {

    private static final String MENU_TITLE = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "변신할 블럭을 선택하세요!";

    private final BlockHideAndSeek plugin;
    private final Set<UUID> openMenus = new HashSet<>();
    private final Map<UUID, BukkitTask> selectionTimers = new HashMap<>();
    private final Map<UUID, List<DisguiseBlock>> openChoices = new HashMap<>();
    private final Set<String> warnedInvalidBlocks = new HashSet<>();
    private BukkitTask changeTask;

    public BlockSelectMenu(BlockHideAndSeek plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        List<DisguiseBlock> allChoices = loadChoices(player);
        int count = plugin.getConfig().getInt("mode2.choices-count", 5);
        List<DisguiseBlock> chosen = pickRandom(allChoices, count);

        if (chosen.isEmpty()) {
            player.sendMessage(ChatColor.RED + "선택 가능한 블럭이 없습니다. config.yml을 확인하세요.");
            return;
        }

        int invSize = getInventorySize(chosen.size());
        if (chosen.size() > invSize) {
            chosen = new ArrayList<>(chosen.subList(0, invSize));
        }

        Inventory inv = Bukkit.createInventory(null, invSize, MENU_TITLE);
        for (int i = 0; i < chosen.size(); i++) {
            inv.setItem(i, createMenuItem(chosen.get(i)));
        }

        UUID uuid = player.getUniqueId();
        openMenus.add(uuid);
        openChoices.put(uuid, chosen);
        player.openInventory(inv);

        startSelectionTimer(player, chosen);
    }

    public void applyMode1Default(Player player) {
        List<DisguiseBlock> choices = loadChoices(player);
        if (!choices.isEmpty()) {
            plugin.getDisguiseManager().disguise(player, choices.get(0));
        }
    }

    private ItemStack createMenuItem(DisguiseBlock block) {
        ItemStack item = block.createIcon();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            ChatColor color = block.isCustom() ? ChatColor.AQUA : ChatColor.GREEN;
            meta.setDisplayName(color + block.displayName());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "클릭하여 변신!");
            if (block.isCustom()) {
                lore.add(ChatColor.DARK_GRAY + block.id());
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void startSelectionTimer(Player player, List<DisguiseBlock> choices) {
        cancelSelectionTimer(player);

        int limit = plugin.getConfig().getInt("mode2.selection-time", 30);

        BukkitTask task = new BukkitRunnable() {
            int remaining = limit;

            @Override
            public void run() {
                UUID uuid = player.getUniqueId();
                if (!openMenus.contains(uuid)) {
                    cancel();
                    return;
                }
                if (remaining <= 0) {
                    cancel();
                    selectionTimers.remove(uuid);
                    openMenus.remove(uuid);
                    openChoices.remove(uuid);
                    player.closeInventory();

                    DisguiseBlock random = choices.get(new Random().nextInt(choices.size()));
                    plugin.getDisguiseManager().disguise(player, random);
                    player.sendMessage(ChatColor.YELLOW + "시간 초과! " + random.displayName() + " 으로 랜덤 변신되었습니다.");
                    return;
                }

                String bar = ChatColor.YELLOW + "블럭을 선택하세요! " + ChatColor.RED + remaining + "초";
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(bar));
                if (remaining <= 3) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                }
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        selectionTimers.put(player.getUniqueId(), task);
    }

    public void cancelSelectionTimer(Player player) {
        BukkitTask t = selectionTimers.remove(player.getUniqueId());
        if (t != null) t.cancel();
    }

    public void startChangeTask() {
        stopChangeTask();

        int interval = plugin.getConfig().getInt("mode2.change-interval", 60);

        changeTask = new BukkitRunnable() {
            int countdown = interval;

            @Override
            public void run() {
                if (plugin.getGameManager().getState() != GameManager.GameState.SEEKING) {
                    cancel();
                    return;
                }

                if (countdown <= 10 && countdown > 0) {
                    for (UUID uid : plugin.getGameManager().getHiders()) {
                        Player p = Bukkit.getPlayer(uid);
                        if (p == null) continue;
                        String bar = ChatColor.RED + "" + countdown + "초 후 블럭 변경!";
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(bar));
                        if (countdown <= 3) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                        }
                    }
                }

                if (countdown <= 0) {
                    for (UUID uid : plugin.getGameManager().getHiders()) {
                        Player p = Bukkit.getPlayer(uid);
                        if (p == null || p.getGameMode() == GameMode.SPECTATOR) continue;
                        plugin.getDisguiseManager().toggleSolidify(p, false);
                        open(p);
                        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    }
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "숨는 사람들의 블럭이 바뀝니다!");
                    countdown = interval;
                } else {
                    countdown--;
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void stopChangeTask() {
        if (changeTask != null) {
            changeTask.cancel();
            changeTask = null;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isSelectionMenu(event)) return;

        event.setCancelled(true);

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) return;

        List<DisguiseBlock> choices = openChoices.get(player.getUniqueId());
        if (choices == null || rawSlot >= choices.size()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        DisguiseBlock selected = choices.get(rawSlot);
        cancelSelectionTimer(player);
        openMenus.remove(player.getUniqueId());
        openChoices.remove(player.getUniqueId());
        player.closeInventory();

        plugin.getDisguiseManager().disguise(player, selected);
        player.sendMessage(ChatColor.GREEN + selected.displayName() + " 블럭으로 변신했습니다.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!MENU_TITLE.equals(event.getView().getTitle())) return;
        if (!openMenus.contains(player.getUniqueId())) return;

        // 아무것도 고르지 않고 닫으면 타이머가 랜덤 선택을 처리합니다.
    }

    private boolean isSelectionMenu(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return false;
        return openMenus.contains(player.getUniqueId()) && MENU_TITLE.equals(event.getView().getTitle());
    }

    private List<DisguiseBlock> loadChoices(Player player) {
        List<DisguiseBlock> choices = new ArrayList<>();

        for (String name : plugin.getConfig().getStringList("selectable-blocks")) {
            DisguiseBlock block = parseVanillaBlock(name);
            if (block != null) {
                choices.add(block);
            }
        }

        List<String> customIds = plugin.getConfig().getStringList("craftengine.blocks");
        if (!customIds.isEmpty()) {
            CraftEngineHook hook = plugin.getCraftEngineHook();
            if (hook == null || !hook.isAvailable()) {
                warnOnce("craftengine-unavailable", "craftengine.blocks가 설정됐지만 CraftEngine을 사용할 수 없습니다.");
            } else {
                for (String id : customIds) {
                    DisguiseBlock block = hook.createBlock(id, player);
                    if (block != null) {
                        choices.add(block);
                    }
                }
            }
        }

        return choices;
    }

    private DisguiseBlock parseVanillaBlock(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        try {
            Material mat = Material.valueOf(name.trim().toUpperCase(Locale.ROOT));
            if (mat.isBlock()) {
                return DisguiseBlock.vanilla(mat);
            }
            warnOnce("not-block:" + name, "selectable-blocks 항목이 블럭이 아닙니다: " + name);
        } catch (IllegalArgumentException ignored) {
            warnOnce("invalid:" + name, "잘못된 selectable-blocks 항목입니다: " + name);
        }
        return null;
    }

    private List<DisguiseBlock> pickRandom(List<DisguiseBlock> choices, int count) {
        List<DisguiseBlock> valid = new ArrayList<>(choices);
        Collections.shuffle(valid);
        int max = Math.min(Math.max(count, 1), valid.size());
        return new ArrayList<>(valid.subList(0, max));
    }

    private int getInventorySize(int itemCount) {
        int rows = Math.max(1, (int) Math.ceil(itemCount / 9.0));
        return Math.min(54, rows * 9);
    }

    private void warnOnce(String key, String message) {
        if (warnedInvalidBlocks.add(key)) {
            plugin.getLogger().warning(message);
        }
    }
}
