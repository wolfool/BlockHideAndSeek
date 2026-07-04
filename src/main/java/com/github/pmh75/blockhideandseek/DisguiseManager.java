package com.github.pmh75.blockhideandseek;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DisguiseManager {

    private final BlockHideAndSeek plugin;
    private final Map<UUID, DisguiseInfo> disguises = new HashMap<>();

    public DisguiseManager(BlockHideAndSeek plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    public void disguise(Player player, Material material) {
        if (disguises.containsKey(player.getUniqueId())) {
            undisguise(player);
        }

        // Spawn BlockDisplay
        Location spawnLoc = player.getLocation();
        BlockDisplay display = (BlockDisplay) player.getWorld().spawnEntity(spawnLoc, EntityType.BLOCK_DISPLAY);
        display.setBlock(Bukkit.createBlockData(material));
        
        // Center the block display to the player (default is corner)
        // BlockDisplay origin is a corner, so we translate it by -0.5, 0, -0.5 so it centers on the player
        Transformation transform = display.getTransformation();
        transform.getTranslation().set(-0.5f, 0f, -0.5f);
        display.setTransformation(transform);
        
        // Make teleportation smooth (1 tick interpolation)
        display.setTeleportDuration(1);

        player.setInvisible(true);

        DisguiseInfo info = new DisguiseInfo(display, material);
        disguises.put(player.getUniqueId(), info);
    }

    public void undisguise(Player player) {
        DisguiseInfo info = disguises.remove(player.getUniqueId());
        if (info != null) {
            info.display.remove();
        }
        player.setInvisible(false);
    }

    public boolean isDisguised(Player player) {
        return disguises.containsKey(player.getUniqueId());
    }

    public boolean isSolidified(Player player) {
        DisguiseInfo info = disguises.get(player.getUniqueId());
        return info != null && info.isSolidified;
    }

    public void toggleSolidify(Player player, boolean isSneaking) {
        DisguiseInfo info = disguises.get(player.getUniqueId());
        if (info == null) return;

        info.isSolidified = isSneaking;
        
        if (isSneaking) {
            info.display.setTeleportDuration(0); // instant snap
        } else {
            info.display.setTeleportDuration(1); // smooth follow
        }
    }

    public void cleanupAll() {
        for (DisguiseInfo info : disguises.values()) {
            info.display.remove();
        }
        disguises.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setInvisible(false);
        }
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, DisguiseInfo> entry : disguises.entrySet()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p == null || !p.isOnline()) continue;

                    DisguiseInfo info = entry.getValue();
                    Location pLoc = p.getLocation();
                    
                    if (!info.isSolidified) {
                        // Smoothly follow player
                        info.display.teleport(pLoc);
                    } else {
                        // Snap to current grid
                        Location snapLoc = new Location(
                                pLoc.getWorld(),
                                pLoc.getBlockX() + 0.5,
                                pLoc.getBlockY(),
                                pLoc.getBlockZ() + 0.5
                        );
                        info.display.teleport(snapLoc);
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private static class DisguiseInfo {
        BlockDisplay display;
        Material material;
        boolean isSolidified = false;

        public DisguiseInfo(BlockDisplay display, Material material) {
            this.display = display;
            this.material = material;
        }
    }
}
