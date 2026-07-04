package com.github.pmh75.blockhideandseek;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public class BlockSelectMenu implements Listener {

    private final BlockHideAndSeek plugin;
    private final Set<UUID> openMenus = new HashSet<>();

    public BlockSelectMenu(BlockHideAndSeek plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        List<String> blockList = plugin.getConfig().getStringList("selectable-blocks");
        int size = Math.min(54, ((blockList.size() / 9) + 1) * 9);
        Inventory inv = Bukkit.createInventory(null, size, ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "변신할 블럭을 선택하세요!");

        for (String blockName : blockList) {
            try {
                Material mat = Material.valueOf(blockName.toUpperCase());
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GREEN + mat.name());
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "클릭하여 이 블럭으로 변신!");
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inv.addItem(item);
            } catch (IllegalArgumentException ignored) {}
        }

        openMenus.add(player.getUniqueId());
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openMenus.contains(player.getUniqueId())) return;
        if (!event.getView().getTitle().contains("변신할 블럭을 선택")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Material chosen = clicked.getType();
        plugin.getDisguiseManager().disguise(player, chosen);
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + chosen.name() + " 블럭으로 변신했습니다!");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!event.getView().getTitle().contains("변신할 블럭을 선택")) return;

        // 아무것도 선택 안 하고 닫으면 기본 블럭(BOOKSHELF)으로 변신
        if (openMenus.contains(player.getUniqueId())) {
            openMenus.remove(player.getUniqueId());
            if (!plugin.getDisguiseManager().isDisguised(player)) {
                plugin.getDisguiseManager().disguise(player, Material.BOOKSHELF);
                player.sendMessage(ChatColor.GRAY + "선택하지 않아 기본 블럭(책장)으로 변신했습니다.");
            }
        }
    }
}
