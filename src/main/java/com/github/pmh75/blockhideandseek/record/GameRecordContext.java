package com.github.pmh75.blockhideandseek.record;

import com.github.pmh75.blockhideandseek.stats.PlayerGameStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GameRecordContext {

    private final Map<UUID, PlayerGameStats> allStats;
    private final Set<UUID> seekers;
    private final Set<UUID> hiders;

    public GameRecordContext(Map<UUID, PlayerGameStats> allStats, Set<UUID> seekers, Set<UUID> hiders) {
        this.allStats = allStats;
        this.seekers = seekers;
        this.hiders = hiders;
    }

    public Map<UUID, PlayerGameStats> allStats() {
        return allStats;
    }

    public Set<UUID> seekers() {
        return seekers;
    }

    public Set<UUID> hiders() {
        return hiders;
    }

    public Player getPlayer(UUID id) {
        return Bukkit.getPlayer(id);
    }
}
