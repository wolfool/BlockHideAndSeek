package com.github.pmh75.blockhideandseek.system;

import com.github.pmh75.blockhideandseek.BlockHideAndSeek;
import com.github.pmh75.blockhideandseek.GameManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class ProximityWarningSystem {

    private final BlockHideAndSeek plugin;
    private BukkitTask task;

    public ProximityWarningSystem(BlockHideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();

        int checkTick = plugin.getConfig().getInt("proximity.check-tick", 5);
        if (checkTick < 1) {
            checkTick = 5;
        }

        int finalCheckTick = checkTick;
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            GameManager gm = plugin.getGameManager();
            if (gm.getState() != GameManager.GameState.SEEKING) {
                return;
            }

            double range8 = plugin.getConfig().getDouble("proximity.range-8.distance", 8);
            double range5 = plugin.getConfig().getDouble("proximity.range-5.distance", 5);
            double range3 = plugin.getConfig().getDouble("proximity.range-3.distance", 3);

            for (Player hider : plugin.getServer().getOnlinePlayers()) {
                if (!gm.getHiders().contains(hider.getUniqueId())) {
                    continue;
                }
                if (hider.getGameMode() == GameMode.SPECTATOR) {
                    continue;
                }

                double nearest = getNearestSeekerDistance(hider, gm);
                if (nearest < 0) {
                    continue;
                }

                if (nearest <= range3 && plugin.getConfig().getBoolean("proximity.range-3.enabled", true)) {
                    applyRange3(hider);
                } else if (nearest <= range5 && plugin.getConfig().getBoolean("proximity.range-5.enabled", true)) {
                    applyRange5(hider);
                } else if (nearest <= range8 && plugin.getConfig().getBoolean("proximity.range-8.enabled", true)) {
                    applyRange8(hider);
                }
            }
        }, finalCheckTick, finalCheckTick);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private double getNearestSeekerDistance(Player hider, GameManager gm) {
        Location hiderLoc = hider.getLocation();
        double nearest = -1;

        for (java.util.UUID seekerId : gm.getSeekers()) {
            Player seeker = plugin.getServer().getPlayer(seekerId);
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

    private void applyRange8(Player hider) {
        if (plugin.getConfig().getBoolean("proximity.range-8.heartbeat-sound", true)) {
            hider.playSound(hider.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 1.2f);
        }
    }

    private void applyRange5(Player hider) {
        if (plugin.getConfig().getBoolean("proximity.range-5.screen-shake", true)) {
            Vector velocity = hider.getVelocity();
            hider.setVelocity(velocity.add(new Vector(
                    (Math.random() - 0.5) * 0.08,
                    0,
                    (Math.random() - 0.5) * 0.08
            )));
        }
    }

    private void applyRange3(Player hider) {
        if (plugin.getConfig().getBoolean("proximity.range-3.warning-sound", true)) {
            hider.playSound(hider.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
        }
        if (plugin.getConfig().getBoolean("proximity.range-3.particles", true)) {
            hider.getWorld().spawnParticle(
                    Particle.CRIT,
                    hider.getLocation().add(0, 1, 0),
                    8,
                    0.4, 0.4, 0.4,
                    0.02
            );
        }
    }
}
