package com.github.pmh75.blockhideandseek;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
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

    // ─────────────────────────────────────────────
    //  변신
    // ─────────────────────────────────────────────

    public void disguise(Player player, Material material) {
        undisguise(player); // 기존 위장 제거

        Location spawnLoc = player.getLocation();
        BlockDisplay display = (BlockDisplay) player.getWorld().spawnEntity(spawnLoc, EntityType.BLOCK_DISPLAY);
        display.setBlock(Bukkit.createBlockData(material));

        // 블럭 디스플레이는 모서리 기준이므로 -0.5 오프셋 적용
        Transformation transform = display.getTransformation();
        transform.getTranslation().set(-0.5f, 0f, -0.5f);
        display.setTransformation(transform);
        display.setTeleportDuration(1);
        display.setShadowRadius(0f);
        display.setShadowStrength(0f);

        int gameMode = plugin.getConfig().getInt("game-mode", 2);
        if (gameMode == 1) {
            // 모드 1: 쉬프트 안 누를 때는 본체 보임, 블럭 숨김
            display.setVisibleByDefault(false);
            showPlayerToAll(player);
            player.setInvisible(false);
        } else {
            // 모드 2: 항상 블럭 상태, 본체 숨김(갑옷/아이템 포함)
            display.setVisibleByDefault(true);
            hidePlayerFromAll(player);
            player.setInvisible(true);
        }

        disguises.put(player.getUniqueId(), new DisguiseInfo(display, material));
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
        }
        showPlayerToAll(player);
        player.setInvisible(false);
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
        return info == null ? null : info.material;
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

    // ─────────────────────────────────────────────
    //  쉬프트 고정 처리
    // ─────────────────────────────────────────────

    /**
     * @param isSneaking true = 고정 시도, false = 고정 해제
     * @return false = 고정 실패 (발 아래 블럭 없음, 모드 1 전용)
     */
    public boolean toggleSolidify(Player player, boolean isSneaking) {
        DisguiseInfo info = disguises.get(player.getUniqueId());
        if (info == null) return true;

        int gameMode = plugin.getConfig().getInt("game-mode", 2);

        if (!isSneaking) {
            // 고정 해제
            info.isSolidified = false;
            if (gameMode == 1) {
                // 모드 1: 블럭 숨기고 본체 표시
                info.display.setVisibleByDefault(false);
                Bukkit.getOnlinePlayers().forEach(op -> op.hideEntity(plugin, info.display));
                showPlayerToAll(player);
                player.setInvisible(false);
            }
            if (info.hitbox != null) {
                info.hitbox.remove();
                info.hitbox = null;
            }
            info.display.setTeleportDuration(1);
            return true;
        }

        // 고정 시도
        if (gameMode == 1) {
            // 모드 1: 발 아래 블럭에 고정
            Location pLoc = player.getLocation();
            Block blockBelow = pLoc.getWorld().getBlockAt(pLoc.getBlockX(), pLoc.getBlockY() - 1, pLoc.getBlockZ());

            if (blockBelow.getType().isAir() || !blockBelow.getType().isSolid()) {
                return false;
            }

            // 블럭 표시 + 고정 + 본체(갑옷 등) 숨기기
            info.isSolidified = true;
            info.display.setTeleportDuration(0);
            info.display.setVisibleByDefault(true);
            Bukkit.getOnlinePlayers().forEach(op -> op.showEntity(plugin, info.display));
            hidePlayerFromAll(player);
            player.setInvisible(true);

            // 즉시 발 아래 블럭 위치로 이동
            Location snapLoc = new Location(
                    pLoc.getWorld(),
                    pLoc.getBlockX() + 0.5,
                    blockBelow.getY() + 1.0,
                    pLoc.getBlockZ() + 0.5
            );
            info.display.teleport(snapLoc);
            createHitbox(player, info, snapLoc);
        } else {
            // 모드 2: 현재 위치 격자에 고정
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
        for (DisguiseInfo info : disguises.values()) {
            info.display.remove();
            if (info.hitbox != null) info.hitbox.remove();
        }
        disguises.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            showPlayerToAll(p);
            p.setInvisible(false);
        }
    }

    // ─────────────────────────────────────────────
    //  매 틱 위치 업데이트
    // ─────────────────────────────────────────────

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                int gameMode = plugin.getConfig().getInt("game-mode", 2);

                for (Map.Entry<UUID, DisguiseInfo> entry : disguises.entrySet()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p == null || !p.isOnline()) continue;

                    DisguiseInfo info = entry.getValue();
                    Location pLoc = p.getLocation();

                    if (!info.isSolidified) {
                        if (gameMode == 1) {
                            // 모드 1: 쉬프트 안 누르면 블럭 안 보임 (아무것도 안 함)
                        } else {
                            // 모드 2: 부드럽게 따라다님
                            info.display.teleport(pLoc);
                        }
                    } else if (gameMode == 1) {
                        // 모드 1: 쉬프트 중 - 발 아래 블럭 위에 고정
                        Block blockBelow = pLoc.getWorld().getBlockAt(
                                pLoc.getBlockX(), pLoc.getBlockY() - 1, pLoc.getBlockZ());

                        if (blockBelow.getType().isAir() || !blockBelow.getType().isSolid()) {
                            // 발 아래 없어지면 고정 해제 + 블럭 숨기기
                            info.isSolidified = false;
                            info.display.setVisibleByDefault(false);
                            Bukkit.getOnlinePlayers().forEach(op -> op.hideEntity(plugin, info.display));
                            showPlayerToAll(p);
                            p.setInvisible(false);
                            if (info.hitbox != null) {
                                info.hitbox.remove();
                                info.hitbox = null;
                            }
                            p.sendActionBar(net.kyori.adventure.text.Component.text(
                                    "§c⚠ 발 아래 블럭이 없어 고정이 풀렸습니다!"));
                        } else {
                            Location snapLoc = new Location(
                                    pLoc.getWorld(),
                                    pLoc.getBlockX() + 0.5,
                                    blockBelow.getY() + 1.0,
                                    pLoc.getBlockZ() + 0.5
                            );
                            info.display.teleport(snapLoc);
                            if (info.hitbox != null) info.hitbox.teleport(snapLoc);
                        }
                    } else {
                        // 모드 2: 현재 위치 격자에 고정 (이동 시 격자 단위로 이동)
                        Location snapLoc = new Location(
                                pLoc.getWorld(),
                                pLoc.getBlockX() + 0.5,
                                pLoc.getBlockY(),
                                pLoc.getBlockZ() + 0.5
                        );
                        info.display.teleport(snapLoc);
                        if (info.hitbox != null) info.hitbox.teleport(snapLoc);
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // ─────────────────────────────────────────────
    //  내부 데이터
    // ─────────────────────────────────────────────

    private static class DisguiseInfo {
        BlockDisplay display;
        Material material;
        boolean isSolidified = false;
        org.bukkit.entity.Shulker hitbox;

        DisguiseInfo(BlockDisplay display, Material material) {
            this.display = display;
            this.material = material;
        }
    }
}
