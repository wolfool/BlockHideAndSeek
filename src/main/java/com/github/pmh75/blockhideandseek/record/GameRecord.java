package com.github.pmh75.blockhideandseek.record;

import com.github.pmh75.blockhideandseek.stats.PlayerGameStats;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.UUID;

public interface GameRecord {

    String id();

    String displayTitle();

    String displayDescription();

    boolean isEligible(Player player, PlayerGameStats stats, GameRecordContext context);

    Comparator<PlayerGameStats> comparator();

    default UUID pickBestPlayer(GameRecordContext context) {
        return context.allStats().values().stream()
                .filter(stats -> {
                    Player player = context.getPlayer(stats.getPlayerId());
                    return player != null && isEligible(player, stats, context);
                })
                .max(comparator())
                .map(PlayerGameStats::getPlayerId)
                .orElse(null);
    }
}
