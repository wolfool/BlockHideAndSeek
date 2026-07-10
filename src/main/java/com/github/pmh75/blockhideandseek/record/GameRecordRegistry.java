package com.github.pmh75.blockhideandseek.record;

import com.github.pmh75.blockhideandseek.stats.PlayerGameStats;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameRecordRegistry {

    private static final String DEFAULT_TITLE = "⭐ 끝까지 최선을 다했습니다.";
    private static final String DEFAULT_DESCRIPTION = "게임 내내 열심히 플레이했습니다.";

    private final List<GameRecord> records = new ArrayList<>();

    public GameRecordRegistry() {
        registerDefaults();
    }

    public void register(GameRecord record) {
        records.add(record);
    }

    public Map<UUID, AwardedRecord> awardAll(GameRecordContext context) {
        Map<UUID, AwardedRecord> awarded = new LinkedHashMap<>();
        List<UUID> participants = new ArrayList<>(context.allStats().keySet());

        for (GameRecord record : records) {
            UUID best = record.pickBestPlayer(context);
            if (best == null || awarded.containsKey(best)) {
                continue;
            }
            awarded.put(best, new AwardedRecord(record.displayTitle(), record.displayDescription()));
        }

        for (UUID participant : participants) {
            if (!awarded.containsKey(participant)) {
                awarded.put(participant, new AwardedRecord(DEFAULT_TITLE, DEFAULT_DESCRIPTION));
            }
        }

        return awarded;
    }

    private void registerDefaults() {
        register(new GameRecord() {
            @Override
            public String id() {
                return "longest_survival";
            }

            @Override
            public String displayTitle() {
                return "🕒 가장 오래 생존했습니다.";
            }

            @Override
            public String displayDescription() {
                return "가장 오래 생존한 플레이어";
            }

            @Override
            public boolean isEligible(Player player, PlayerGameStats stats, GameRecordContext context) {
                return context.hiders().contains(player.getUniqueId()) && stats.getSurvivalTicks() > 0;
            }

            @Override
            public Comparator<PlayerGameStats> comparator() {
                return Comparator.comparingLong(PlayerGameStats::getSurvivalTicks);
            }
        });

        register(new GameRecord() {
            @Override
            public String id() {
                return "most_discoveries";
            }

            @Override
            public String displayTitle() {
                return "🔍 가장 많은 플레이어를 발견했습니다.";
            }

            @Override
            public String displayDescription() {
                return "술래 중 가장 많이 발견한 플레이어";
            }

            @Override
            public boolean isEligible(Player player, PlayerGameStats stats, GameRecordContext context) {
                return context.seekers().contains(player.getUniqueId()) && stats.getDiscoveries() > 0;
            }

            @Override
            public Comparator<PlayerGameStats> comparator() {
                return Comparator.comparingInt(PlayerGameStats::getDiscoveries);
            }
        });

        register(new GameRecord() {
            @Override
            public String id() {
                return "highest_danger";
            }

            @Override
            public String displayTitle() {
                return "⚠️ 가장 위험한 상황을 많이 버텼습니다.";
            }

            @Override
            public String displayDescription() {
                return "위험도 점수가 가장 높은 플레이어";
            }

            @Override
            public boolean isEligible(Player player, PlayerGameStats stats, GameRecordContext context) {
                return context.hiders().contains(player.getUniqueId()) && stats.getDangerScore() > 0;
            }

            @Override
            public Comparator<PlayerGameStats> comparator() {
                return Comparator.comparingInt(PlayerGameStats::getDangerScore);
            }
        });

        register(new GameRecord() {
            @Override
            public String id() {
                return "most_emotes";
            }

            @Override
            public String displayTitle() {
                return "🎭 가장 많이 도발했습니다.";
            }

            @Override
            public String displayDescription() {
                return "도발 사용 횟수가 가장 많은 플레이어";
            }

            @Override
            public boolean isEligible(Player player, PlayerGameStats stats, GameRecordContext context) {
                return context.hiders().contains(player.getUniqueId()) && stats.getEmoteCount() > 0;
            }

            @Override
            public Comparator<PlayerGameStats> comparator() {
                return Comparator.comparingInt(PlayerGameStats::getEmoteCount);
            }
        });
    }

    public record AwardedRecord(String title, String description) { }
}
