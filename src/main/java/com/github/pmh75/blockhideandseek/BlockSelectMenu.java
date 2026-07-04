package com.github.pmh75.blockhideandseek;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class BlockSelectMenu implements Listener {

    private final BlockHideAndSeek plugin;
    private final Set<UUID> openMenus = new HashSet<>();
    // 플레이어별 선택 제한 타이머
    private final Map<UUID, BukkitTask> selectionTimers = new HashMap<>();
    // 강제 변경 주기 타이머 (모드 2)
    private BukkitTask changeTask;

    public BlockSelectMenu(BlockHideAndSeek plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ─────────────────────────────────────────────
    //  공통: 블럭 선택 GUI 열기
    // ─────────────────────────────────────────────

    public void open(Player player) {
        // 선택지 수만큼 랜덤 블럭 추출
        List<String> all = plugin.getConfig().getStringList("selectable-blocks");
        int count = plugin.getConfig().getInt("mode2.choices-count", 5);
        List<Material> chosen = pickRandom(all, count);

        // 인벤토리 크기: 선택지가 9개 이하면 9, 아니면 18
        int invSize = chosen.size() <= 9 ? 9 : 18;
        Inventory inv = Bukkit.createInventory(null, invSize,
                ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "변신할 블럭을 선택하세요!");

        for (Material mat : chosen) {
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + mat.name());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "클릭하여 변신!");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.addItem(item);
        }

        openMenus.add(player.getUniqueId());
        player.openInventory(inv);

        // 선택 시간 제한 시작
        startSelectionTimer(player, chosen);
    }

    // 모드 1용: 메뉴 없이 바로 기본 블럭으로 변신
    public void applyMode1Default(Player player) {
        List<String> all = plugin.getConfig().getStringList("selectable-blocks");
        if (all.isEmpty()) return;
        try {
            Material mat = Material.valueOf(all.get(0).toUpperCase());
            plugin.getDisguiseManager().disguise(player, mat);
        } catch (IllegalArgumentException ignored) {}
    }

    // ─────────────────────────────────────────────
    //  선택 제한 타이머 (모드 2)
    // ─────────────────────────────────────────────

    private void startSelectionTimer(Player player, List<Material> choices) {
        cancelSelectionTimer(player);

        int limit = plugin.getConfig().getInt("mode2.selection-time", 10);

        BukkitTask task = new BukkitRunnable() {
            int remaining = limit;

            @Override
            public void run() {
                if (!openMenus.contains(player.getUniqueId())) {
                    cancel();
                    return;
                }
                if (remaining <= 0) {
                    // 시간 초과 → 랜덤 선택
                    cancel();
                    openMenus.remove(player.getUniqueId());
                    player.closeInventory();

                    Material random = choices.isEmpty()
                            ? Material.BOOKSHELF
                            : choices.get(new Random().nextInt(choices.size()));
                    plugin.getDisguiseManager().disguise(player, random);
                    player.sendMessage(ChatColor.YELLOW + "⏰ 시간 초과! " + random.name() + " 으로 랜덤 변신되었습니다.");
                    return;
                }

                // 액션바 카운트다운
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

    // ─────────────────────────────────────────────
    //  모드 2: 주기적 강제 블럭 변경
    // ─────────────────────────────────────────────

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

                // 10초 전부터 경고
                if (countdown <= 10 && countdown > 0) {
                    for (UUID uid : plugin.getGameManager().getHiders()) {
                        Player p = Bukkit.getPlayer(uid);
                        if (p == null) continue;
                        String bar = ChatColor.RED + "⚠ " + countdown + "초 후 블럭 변경!";
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(bar));
                        if (countdown <= 3) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                        }
                    }
                }

                if (countdown <= 0) {
                    // 블럭 변경!
                    for (UUID uid : plugin.getGameManager().getHiders()) {
                        Player p = Bukkit.getPlayer(uid);
                        if (p == null || p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
                        // 고정 해제 후 GUI 열기
                        plugin.getDisguiseManager().toggleSolidify(p, false);
                        open(p);
                        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    }
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "⚡ 도망자들이 블럭을 바꿔야 합니다!");
                    countdown = interval; // 리셋
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

    // ─────────────────────────────────────────────
    //  인벤토리 이벤트
    // ─────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openMenus.contains(player.getUniqueId())) return;
        if (!event.getView().getTitle().contains("변신할 블럭을 선택")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        cancelSelectionTimer(player);
        openMenus.remove(player.getUniqueId());
        player.closeInventory();

        plugin.getDisguiseManager().disguise(player, clicked.getType());
        player.sendMessage(ChatColor.GREEN + "✔ " + clicked.getType().name() + " 블럭으로 변신했습니다!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!event.getView().getTitle().contains("변신할 블럭을 선택")) return;
        if (!openMenus.contains(player.getUniqueId())) return;

        // 아무것도 안 고르고 닫았을 때 → 타이머가 자동 처리
        // (타이머가 이미 취소됐다면 선택 완료된 것)
    }

    // ─────────────────────────────────────────────
    //  유틸
    // ─────────────────────────────────────────────

    private List<Material> pickRandom(List<String> blockNames, int count) {
        List<Material> valid = new ArrayList<>();
        for (String name : blockNames) {
            try {
                Material mat = Material.valueOf(name.toUpperCase());
                if (mat.isBlock()) valid.add(mat);
            } catch (IllegalArgumentException ignored) {}
        }
        Collections.shuffle(valid);
        return valid.subList(0, Math.min(count, valid.size()));
    }
}
