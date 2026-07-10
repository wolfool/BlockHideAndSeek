package com.github.pmh75.blockhideandseek.stats;

import java.util.UUID;

public class PlayerGameStats {

    private final UUID playerId;
    private long survivalTicks;
    private int discoveries;
    private int dangerScore;
    private int emoteCount;
    private boolean alive = true;

    public PlayerGameStats(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public long getSurvivalTicks() {
        return survivalTicks;
    }

    public void addSurvivalTicks(long ticks) {
        if (alive) {
            survivalTicks += ticks;
        }
    }

    public void markDead() {
        alive = false;
    }

    public boolean isAlive() {
        return alive;
    }

    public int getDiscoveries() {
        return discoveries;
    }

    public void incrementDiscoveries() {
        discoveries++;
    }

    public int getDangerScore() {
        return dangerScore;
    }

    public void addDangerScore(int score) {
        dangerScore += score;
    }

    public int getEmoteCount() {
        return emoteCount;
    }

    public void incrementEmoteCount() {
        emoteCount++;
    }
}
