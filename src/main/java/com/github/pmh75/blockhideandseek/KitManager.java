package com.github.pmh75.blockhideandseek;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class KitManager implements Listener {

    private final BlockHideAndSeek plugin;
    private final File kitFile;
    private FileConfiguration kitConfig;

    public KitManager(BlockHideAndSeek plugin) {
        this.plugin = plugin;
        this.kitFile = new File(plugin.getDataFolder(), "kits.yml");
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void loadConfig() {
        if (!kitFile.exists()) {
            try {
                kitFile.getParentFile().mkdirs();
                kitFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        kitConfig = YamlConfiguration.loadConfiguration(kitFile);
    }

    public void saveConfig() {
        try {
            kitConfig.save(kitFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openKitEditor(Player player, String role) {
        if (!role.equalsIgnoreCase("hider") && !role.equalsIgnoreCase("seeker")) {
            player.sendMessage(ChatColor.RED + "역할은 hider 또는 seeker 중 하나여야 합니다.");
            return;
        }

        String title = ChatColor.BOLD + (role.equalsIgnoreCase("hider") ? "도망자" : "술래") + " 키트 설정";
        Inventory inv = Bukkit.createInventory(player, 36, title);

        // Load existing items
        if (kitConfig.contains("kits." + role.toLowerCase())) {
            List<ItemStack> items = (List<ItemStack>) kitConfig.getList("kits." + role.toLowerCase());
            if (items != null) {
                for (int i = 0; i < items.size() && i < inv.getSize(); i++) {
                    inv.setItem(i, items.get(i));
                }
            }
        }

        player.openInventory(inv);
    }

    public void giveKit(Player player, String role) {
        if (kitConfig.contains("kits." + role.toLowerCase())) {
            List<ItemStack> items = (List<ItemStack>) kitConfig.getList("kits." + role.toLowerCase());
            if (items != null) {
                for (ItemStack item : items) {
                    if (item != null) {
                        player.getInventory().addItem(item);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (title.contains("키트 설정")) {
            String role = title.contains("도망자") ? "hider" : "seeker";
            Inventory inv = event.getInventory();
            
            // Save contents to config
            ItemStack[] contents = inv.getContents();
            kitConfig.set("kits." + role, contents);
            saveConfig();
            
            event.getPlayer().sendMessage(ChatColor.GREEN + role.toUpperCase() + " 키트가 저장되었습니다!");
        }
    }
}
