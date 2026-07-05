package com.github.pmh75.blockhideandseek;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;

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
        disguise(player, DisguiseBlock.vanilla(material));
    }

    public void disguise(Player player, DisguiseBlock disguise) {
        disguise(player, disguise, false);
    }

    public void disguiseAsBlock(Player player, DisguiseBlock disguise) {
        disguise(player, disguise, true);
    }

    private void disguise(Player player, DisguiseBlock disguise, boolean forceBlockMode) {
        undisguise(player);

        Location spawnLoc = player.getLocation();
        BlockDisplay display = (BlockDisplay) player.getWorld().spawnEntity(spawnLoc, EntityType.BLOCK_DISPLAY);
        display.setBlock(disguise.createBlockData());

        Transformation transform = display.getTransformation();
        transform.getTranslation().set(-0.5f, 0f, -0.5f);
        display.setTransformation(transform);
        display.setTeleportDuration(1);
        display.setShadowRadius(0f);
        display.setShadowStrength(0f);
        display.setVisibleByDefault(false);

        int gameMode = plugin.getConfig().getInt("game-mode", 2);
        boolean blockMode = forceBlockMode || gameMode == 2;
        if (blockMode) {
            hidePlayerFromAll(player);
            Bukkit.getOnlinePlayers().forEach(op -> {
                if (!op.equals(player)) {
                    op.showEntity(plugin, display);
                }
            });
        } else {
            showPlayerToAll(player);
        }
        player.setInvisible(blockMode);

        player.hideEntity(plugin, display);

        ItemStack originalHelmet = player.getEquipment().getHelmet();
        DisguiseInfo newInfo = new DisguiseInfo(display, disguise, originalHelmet, forceBlockMode);
        disguises.put(player.getUniqueId(), newInfo);

        if (blockMode) {
            player.getEquipment().setHelmet(disguise.createHelmetItem());
            createHitbox(player, newInfo, spawnLoc);
        }
    }

    private void hidePlayerFromAll(Player player) {
        for (Player op : Bukkit.getOnlinePlayers()) {
            if (!op.equals(player)) {
                op.hidePlayer(plugin, player);
            }
        }
    }

    private void showPlayerToAll(Player player) {
        for (Player op : Bukkit.getOnlinePlayers()) {
            if (!op.equals(player)) {
                op.showPlayer(plugin, player);
            }
        }
    }

    public void undisguise(Player player) {
        DisguiseInfo info = disguises.remove(player.getUniqueId());
        if (info != null) {
            info.display.remove();
            if (info.hitbox != null) info.hitbox.remove();
            player.getEquipment().setHelmet(info.originalHelmet);
        }
        player.setInvisible(false);
        showPlayerToAll(player);
    }

    public boolean isDisguised(Player player) {
        return disguises.containsKey(player.getUniqueId());
    }

    public boolean isSolidified(Player player) {
        DisguiseInfo info = disguises.get(player.getUniqueId());
        return info != null && info.isSolidified;
    }

    public Material getDisguiseMaterial(Player player) {
        DisguiseInfo info = disguises.get(player.getUniqueId());
        return info == null ? null : info.disguise.material();
    }

    public Player getOwnerOfHitbox(org.bukkit.entity.Entity entity) {
        if (!(entity instanceof org.bukkit.entity.Shulker)) return null;
        for (Map.Entry<UUID, DisguiseInfo> entry : disguises.entrySet()) {
            if (entry.getValue().hitbox != null && entry.getValue().hitbox.equals(entity)) {
                return Bukkit.getPlayer(entry.getKey());
            }
        }
        return null;
    }

    public void refreshVisibilityFor(Player viewer) {
        int gameMode = plugin.getConfig().getInt("game-mode", 2);
        for (Map.Entry<UUID, DisguiseInfo> entry : disguises.entrySet()) {
            Player owner = Bukkit.getPlayer(entry.getKey());
            if (owner == null || owner.equals(viewer)) continue;

            DisguiseInfo info = entry.getValue();
            if (info.forceBlockMode || info.isSolidified || gameMode == 2) {
                viewer.showEntity(plugin, info.display);
                viewer.hidePlayer(plugin, owner);
            }
        }
    }

    /**
     * @param isSneaking true = 고정 시도, false = 고정 해제
     * @return false = 고정 실패
     */
    public boolean toggleSolidify(Player player, boolean isSneaking) {
        DisguiseInfo info = disguises.get(player.getUniqueId());
        if (info == null) return true;

        int gameMode = info.forceBlockMode ? 2 : plugin.getConfig().getInt("game-mode", 2);

        if (!isSneaking) {
            info.isSolidified = false;
            if (gameMode == 1) {
                unsolidifyMode1(player, info);
            }
            info.display.setTeleportDuration(1);
            return true;
        }

        if (gameMode == 1) {
            Location pLoc = player.getLocation();
            Block blockBelow = pLoc.clone().subtract(0, 0.1, 0).getBlock();

            if (!isValidFullBlock(blockBelow)) {
                return false;
            }

            solidifyMode1(player, info, pLoc, blockBelow);
        } else {
            info.isSolidified = true;
            info.display.setTeleportDuration(0);
            Location snapLoc = new Location(
                    player.getWorld(),
                    player.getLocation().getBlockX() + 0.5,
                    player.getLocation().getBlockY(),
                    player.getLocation().getBlockZ() + 0.5
            );
            createHitbox(player, info, snapLoc);
        }
        return true;
    }

    private void createHitbox(Player owner, DisguiseInfo info, Location loc) {
        if (info.hitbox == null) {
            info.hitbox = (org.bukkit.entity.Shulker) loc.getWorld().spawnEntity(loc, EntityType.SHULKER);
            info.hitbox.setInvisible(true);
            info.hitbox.setAI(false);
            info.hitbox.setSilent(true);
            info.hitbox.setGravity(false);
            info.hitbox.setCollidable(false);
            info.hitbox.setInvulnerable(false);
            owner.hideEntity(plugin, info.hitbox);
        } else {
            info.hitbox.teleport(loc);
        }
    }

    public void cleanupAll() {
        for (Map.Entry<UUID, DisguiseInfo> entry : disguises.entrySet()) {
            DisguiseInfo info = entry.getValue();
            info.display.remove();
            if (info.hitbox != null) info.hitbox.remove();
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null) {
                p.getEquipment().setHelmet(info.originalHelmet);
                p.setInvisible(false);
            }
        }
        disguises.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            showPlayerToAll(p);
            p.setInvisible(false);
        }
    }

    private void solidifyMode1(Player player, DisguiseInfo info, Location pLoc, Block blockBelow) {
        DisguiseBlock block = DisguiseBlock.vanilla(blockBelow.getType());
        info.display.setBlock(block.createBlockData());
        info.disguise = block;
        player.getEquipment().setHelmet(block.createHelmetItem());

        info.isSolidified = true;
        info.display.setTeleportDuration(0);
        info.display.setVisibleByDefault(false);
        Bukkit.getOnlinePlayers().forEach(op -> {
            if (!op.equals(player)) {
                op.showEntity(plugin, info.display);
            }
        });
        hidePlayerFromAll(player);

        Location snapLoc = new Location(
                pLoc.getWorld(),
                pLoc.getBlockX() + 0.5,
                blockBelow.getY() + 1.0,
                pLoc.getBlockZ() + 0.5
        );
        info.display.teleport(snapLoc);
        createHitbox(player, info, snapLoc);
    }

    private void unsolidifyMode1(Player player, DisguiseInfo info) {
        info.isSolidified = false;
        info.display.setVisibleByDefault(false);
        Bukkit.getOnlinePlayers().forEach(op -> op.hideEntity(plugin, info.display));
        player.getEquipment().setHelmet(info.originalHelmet);
        showPlayerToAll(player);
        if (info.hitbox != null) {
            info.hitbox.remove();
            info.hitbox = null;
        }
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                int gameMode = plugin.getConfig().getInt("game-mode", 2);
                for (Map.Entry<UUID, DisguiseInfo> entry : disguises.entrySet()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p == null || !p.isOnline()) continue;
                    DisguiseInfo info = entry.getValue();
                    int effectiveGameMode = info.forceBlockMode ? 2 : gameMode;
                    updateDisguiseTick(p, info, effectiveGameMode);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void updateDisguiseTick(Player player, DisguiseInfo info, int gameMode) {
        Location pLoc = player.getLocation();

        if (!info.isSolidified) {
            if (gameMode == 1) {
                tickMode1Idle(player, info, pLoc);
            } else {
                tickMode2Idle(info, pLoc);
            }
        } else if (gameMode == 1) {
            tickMode1Solidified(player, info, pLoc);
        } else {
            tickMode2Solidified(info, pLoc);
        }
    }

    private void tickMode1Idle(Player player, DisguiseInfo info, Location pLoc) {
        if (!player.isSneaking()) return;
        Block blockBelow = pLoc.clone().subtract(0, 0.1, 0).getBlock();
        if (isValidFullBlock(blockBelow)) {
            solidifyMode1(player, info, pLoc, blockBelow);
        }
    }

    private void tickMode2Idle(DisguiseInfo info, Location pLoc) {
        info.display.teleport(pLoc);
        if (info.hitbox != null) {
            info.hitbox.teleport(pLoc);
        }
    }

    private void tickMode1Solidified(Player player, DisguiseInfo info, Location pLoc) {
        Block blockBelow = pLoc.clone().subtract(0, 0.1, 0).getBlock();

        if (!isValidFullBlock(blockBelow)) {
            unsolidifyMode1(player, info);
            player.sendActionBar(net.kyori.adventure.text.Component.text(
                    "발 아래 블럭이 없어 고정이 풀렸습니다!"));
            return;
        }

        updateBlockType(player, info, blockBelow);
        snapToBlock(info, pLoc, blockBelow);
    }

    private void tickMode2Solidified(DisguiseInfo info, Location pLoc) {
        Location snapLoc = new Location(
                pLoc.getWorld(),
                pLoc.getBlockX() + 0.5,
                pLoc.getBlockY(),
                pLoc.getBlockZ() + 0.5
        );
        info.display.teleport(snapLoc);
        if (info.hitbox != null) info.hitbox.teleport(snapLoc);
    }

    private boolean isValidFullBlock(Block block) {
        return !block.getType().isAir()
                && block.getType().isSolid()
                && block.getBoundingBox().getVolume() >= 1.0;
    }

    private void updateBlockType(Player player, DisguiseInfo info, Block blockBelow) {
        if (blockBelow.getType() != info.disguise.material()) {
            DisguiseBlock block = DisguiseBlock.vanilla(blockBelow.getType());
            info.display.setBlock(block.createBlockData());
            info.disguise = block;
            player.getEquipment().setHelmet(block.createHelmetItem());
        }
    }

    private void snapToBlock(DisguiseInfo info, Location pLoc, Block blockBelow) {
        Location snapLoc = new Location(
                pLoc.getWorld(),
                pLoc.getBlockX() + 0.5,
                blockBelow.getY() + 1.0,
                pLoc.getBlockZ() + 0.5
        );
        info.display.teleport(snapLoc);
        if (info.hitbox != null) info.hitbox.teleport(snapLoc);
    }

    private static class DisguiseInfo {
        BlockDisplay display;
        DisguiseBlock disguise;
        boolean isSolidified = false;
        boolean forceBlockMode;
        org.bukkit.entity.Shulker hitbox;
        ItemStack originalHelmet;

        DisguiseInfo(BlockDisplay display, DisguiseBlock disguise, ItemStack originalHelmet, boolean forceBlockMode) {
            this.display = display;
            this.disguise = disguise;
            this.originalHelmet = originalHelmet;
            this.forceBlockMode = forceBlockMode;
        }
    }
}
