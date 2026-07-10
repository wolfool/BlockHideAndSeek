package com.github.pmh75.blockhideandseek.stats;

import com.github.pmh75.blockhideandseek.BlockHideAndSeek;
import com.github.pmh75.blockhideandseek.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameStatsManager {

    private final BlockHideAndSeek plugin;
    private final Map<UUID, PlayerGameStats> stats = new HashMap<>();
    private BukkitTask dangerTask;
    private BukkitTask survivalTask;

    public GameStatsManager(BlockHideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void startTracking(Collection<UUID> participants) {
        stats.clear();
        for (UUID uid : participants) {
            stats.put(uid, new PlayerGameStats(uid));
        }
        startSurvivalTask();
        startDangerTask();
    }

    public void stopTracking() {
        if (survivalTask != null) {
            survivalTask.cancel();
            survivalTask = null;
        }
        if (dangerTask != null) {
            dangerTask.cancel();
            dangerTask = null;
        }
    }

    public Map<UUID, PlayerGameStats> getAllStats() {
        return new HashMap<>(stats);
    }

    public PlayerGameStats getStats(UUID playerId) {
        return stats.get(playerId);
    }

    public void ensureStats(UUID playerId) {
        stats.computeIfAbsent(playerId, PlayerGameStats::new);
    }

    public void recordHiderCaught(Player hider, Player catcher) {
        PlayerGameStats hiderStats = stats.get(hider.getUniqueId());
        if (hiderStats != null) {
            hiderStats.markDead();
        }
        if (catcher != null && plugin.getGameManager().getSeekers().contains(catcher.getUniqueId())) {
            ensureStats(catcher.getUniqueId());
            stats.get(catcher.getUniqueId()).incrementDiscoveries();
        }
    }

    public void recordEmote(Player player) {
        ensureStats(player.getUniqueId());
        stats.get(player.getUniqueId()).incrementEmoteCount();
    }

    private void startSurvivalTask() {
        if (survivalTask != null) {
            survivalTask.cancel();
        }

        survivalTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            GameManager gm = plugin.getGameManager();
            if (gm.getState() != GameManager.GameState.HIDING
                    && gm.getState() != GameManager.GameState.SEEKING) {
                return;
            }

            for (UUID uid : gm.getHiders()) {
                Player hider = Bukkit.getPlayer(uid);
                if (hider == null || hider.getGameMode() == GameMode.SPECTATOR) {
                    continue;
                }
                ensureStats(uid);
                stats.get(uid).addSurvivalTicks(1);
            }
        }, 1L, 1L);
    }

    private void startDangerTask() {
        if (dangerTask != null) {
            dangerTask.cancel();
        }

        int checkTick = plugin.getConfig().getInt("danger.check-tick", 5);
        if (checkTick < 1) {
            checkTick = 5;
        }

        int finalCheckTick = checkTick;
        dangerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            GameManager gm = plugin.getGameManager();
            if (gm.getState() != GameManager.GameState.SEEKING) {
                return;
            }

            int score8 = plugin.getConfig().getInt("danger.range-8", 1);
            int score5 = plugin.getConfig().getInt("danger.range-5", 3);
            int score3 = plugin.getConfig().getInt("danger.range-3", 5);
            int score1 = plugin.getConfig().getInt("danger.range-1", 10);

            for (UUID hiderId : gm.getHiders()) {
                Player hider = Bukkit.getPlayer(hiderId);
                if (hider == null || hider.getGameMode() == GameMode.SPECTATOR) {
                    continue;
                }

                double nearest = getNearestSeekerDistance(hider, gm);
                if (nearest < 0) {
                    continue;
                }

                int add = 0;
                if (nearest <= 1.0) {
                    add = score1;
                } else if (nearest <= 3.0) {
                    add = score3;
                } else if (nearest <= 5.0) {
                    add = score5;
                } else if (nearest <= 8.0) {
                    add = score8;
                }

                if (add > 0) {
                    ensureStats(hiderId);
                    stats.get(hiderId).addDangerScore(add);
                }
            }
        }, finalCheckTick, finalCheckTick);
    }

    private double getNearestSeekerDistance(Player hider, GameManager gm) {
        Location hiderLoc = hider.getLocation();
        double nearest = -1;

        for (UUID seekerId : gm.getSeekers()) {
            Player seeker = Bukkit.getPlayer(seekerId);
            if (seeker == null) {
                continue;
            }
            double dist = seeker.getLocation().distance(hiderLoc);
            if (nearest < 0 || dist < nearest) {
                nearest = dist;
            }
        }
        return nearest;
    }
}
