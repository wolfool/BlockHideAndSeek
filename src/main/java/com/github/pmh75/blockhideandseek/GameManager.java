package com.github.pmh75.blockhideandseek;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GameManager {

    public enum GameState {
        WAITING, HIDING, SEEKING, ENDED
    }

    private final BlockHideAndSeek plugin;

    private GameState state = GameState.WAITING;
    private final Set<UUID> hiders = new HashSet<>();
    private final Set<UUID> seekers = new HashSet<>();
    private final Map<UUID, Integer> hintUsages = new HashMap<>();

    private Location lobbySpawn;
    private Location hiderSpawn;
    private Location seekerSpawn;

    private BossBar bossBar;
    private BukkitRunnable timerTask;
    private int timeLeft;

    // 힌트 사용 횟수 (seeker 당 아닌 게임 전체 공유)
    private int hintsLeft;

    public GameManager(BlockHideAndSeek plugin) {
        this.plugin = plugin;
        this.bossBar = Bukkit.createBossBar(
                ChatColor.YELLOW + "블럭 숨바꼭질 대기 중...",
                BarColor.YELLOW,
                BarStyle.SOLID
        );
    }

    // ─────────────────────────────────────────────
    //  게임 시작
    // ─────────────────────────────────────────────

    public void startGame() {
        if (state != GameState.WAITING) {
            broadcast(ChatColor.RED + "이미 게임이 진행 중입니다.");
            return;
        }

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        int minPlayers = plugin.getConfig().getInt("min-players", 2);
        if (online.size() < minPlayers) {
            broadcast(ChatColor.RED + "게임을 시작하려면 최소 " + minPlayers + "명이 필요합니다.");
            return;
        }

        assignRoles(online);
        hintsLeft = plugin.getConfig().getInt("hints.max-usages", 3);

        // 모든 플레이어 보스바에 추가
        bossBar.removeAll();
        for (Player p : online) {
            bossBar.addPlayer(p);
        }

        int gameMode = plugin.getConfig().getInt("game-mode", 2);
        setupSeekers();
        setupHiders();
        startBlockSelection(gameMode);
        startHidePhase();
    }

    private void assignRoles(List<Player> online) {
        int seekerCount = Math.max(1, online.size() / 4);
        Collections.shuffle(online);

        hiders.clear();
        seekers.clear();

        for (int i = 0; i < online.size(); i++) {
            if (i < seekerCount) {
                seekers.add(online.get(i).getUniqueId());
            } else {
                hiders.add(online.get(i).getUniqueId());
            }
        }
    }

    private void setupSeekers() {
        for (UUID uid : seekers) {
            Player p = Bukkit.getPlayer(uid);
            if (p == null) continue;
            p.sendTitle(ChatColor.RED + "술래!", ChatColor.WHITE + "도망자들을 찾아라!", 10, 60, 20);
            p.sendMessage(ChatColor.RED + "당신은 술래입니다!");
            if (seekerSpawn != null) p.teleport(seekerSpawn);
            p.setGameMode(GameMode.ADVENTURE);
            p.setFoodLevel(19);
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.BLINDNESS, 20 * 5, 1, false, false));
            plugin.getKitManager().giveKit(p, "seeker");
        }
    }

    private void setupHiders() {
        for (UUID uid : hiders) {
            Player p = Bukkit.getPlayer(uid);
            if (p == null) continue;
            p.sendTitle(ChatColor.GREEN + "도망자!", ChatColor.WHITE + "빨리 숨어라!", 10, 60, 20);
            p.sendMessage(ChatColor.GREEN + "당신은 도망자입니다!");
            if (hiderSpawn != null) p.teleport(hiderSpawn);
            p.setGameMode(GameMode.ADVENTURE);
            p.setFoodLevel(19);
            plugin.getKitManager().giveKit(p, "hider");
        }
    }

    private void startBlockSelection(int gameMode) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID uid : hiders) {
                Player p = Bukkit.getPlayer(uid);
                if (p == null) continue;
                if (gameMode == 2) {
                    plugin.getBlockSelectMenu().open(p);
                } else {
                    plugin.getBlockSelectMenu().applyMode1Default(p);
                }
            }
        }, 20L);
    }

    // ─────────────────────────────────────────────
    //  숨기 페이즈
    // ─────────────────────────────────────────────

    private void startHidePhase() {
        state = GameState.HIDING;
        timeLeft = plugin.getConfig().getInt("times.hide-time", 60);

        broadcast(ChatColor.GREEN + "▶ 숨기 시간 시작! 도망자들이 숨을 시간입니다.");

        // 술래 freeze (blindness + 발 못 움직이게)
        for (UUID uid : seekers) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) {
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.BLINDNESS,
                        timeLeft * 20, 1, false, false));
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS,
                        timeLeft * 20, 6, false, false)); // 128 = complete freeze
            }
        }

        startTimer(() -> startSeekPhase());
    }

    // ─────────────────────────────────────────────
    //  술래잡기 페이즈
    // ─────────────────────────────────────────────

    private void startSeekPhase() {
        state = GameState.SEEKING;
        timeLeft = plugin.getConfig().getInt("times.game-time", 300);

        broadcast(ChatColor.RED + "▶ 술래잡기 시작! 도망자를 찾아라!");
        broadcast(ChatColor.YELLOW + "힌트 횟수: " + hintsLeft + "회  →  /bhs hint");

        // 술래 효과 해제
        for (UUID uid : seekers) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) {
                p.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
                p.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
                p.sendTitle(ChatColor.RED + "술래잡기 시작!", "", 10, 40, 20);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
            }
        }

        // 모드 2: 주기적 블럭 변경 태스크 시작
        if (plugin.getConfig().getInt("game-mode", 2) == 2) {
            plugin.getBlockSelectMenu().startChangeTask();
        }

        startTimer(() -> endGame(false));
    }

    // ─────────────────────────────────────────────
    //  게임 종료
    // ─────────────────────────────────────────────

    public void endGame(boolean seekerWin) {
        if (timerTask != null) timerTask.cancel();
        state = GameState.ENDED;

        // 모드 2 변경 타이머 중지
        plugin.getBlockSelectMenu().stopChangeTask();

        if (seekerWin) {
            broadcast(ChatColor.RED + "" + ChatColor.BOLD + "술래 승리! 모든 도망자를 잡았습니다!");
        } else {
            broadcast(ChatColor.GREEN + "" + ChatColor.BOLD + "도망자 승리! 시간이 종료될 때까지 살아남았습니다!");
        }

        // 모든 플레이어 위장 해제 + 게임 모드 복원
        for (UUID uid : hiders) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) {
                plugin.getDisguiseManager().undisguise(p);
                p.setGameMode(GameMode.SURVIVAL);
                p.getInventory().clear();
                if (lobbySpawn != null) p.teleport(lobbySpawn);
            }
        }
        for (UUID uid : seekers) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) {
                p.setGameMode(GameMode.SURVIVAL);
                p.getInventory().clear();
                if (lobbySpawn != null) p.teleport(lobbySpawn);
            }
        }

        hiders.clear();
        seekers.clear();
        bossBar.removeAll();

        // 3초 후 WAITING으로 리셋
        Bukkit.getScheduler().runTaskLater(plugin, () -> state = GameState.WAITING, 60L);
    }

    // ─────────────────────────────────────────────
    //  플레이어 사망 처리
    // ─────────────────────────────────────────────

    public void onHiderDie(Player hider) {
        if (!hiders.contains(hider.getUniqueId())) return;

        plugin.getDisguiseManager().undisguise(hider);
        hider.setGameMode(GameMode.SPECTATOR);
        hider.sendTitle(ChatColor.RED + "잡혔다!", ChatColor.WHITE + "관전자로 전환됩니다.", 10, 60, 20);
        broadcast(ChatColor.RED + hider.getName() + " 이(가) 잡혔습니다! 남은 도망자: " + countAliveHiders() + "명");

        if (countAliveHiders() == 0) {
            endGame(true);
        }

        updateScoreboard();
    }

    private int countAliveHiders() {
        int count = 0;
        for (UUID uid : hiders) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.getGameMode() != GameMode.SPECTATOR) count++;
        }
        return count;
    }

    // ─────────────────────────────────────────────
    //  힌트 시스템
    // ─────────────────────────────────────────────

    public void useHint(Player seeker) {
        if (state != GameState.SEEKING) {
            seeker.sendMessage(ChatColor.RED + "술래잡기 중에만 힌트를 사용할 수 있습니다.");
            return;
        }
        if (!seekers.contains(seeker.getUniqueId())) {
            seeker.sendMessage(ChatColor.RED + "술래만 힌트를 사용할 수 있습니다.");
            return;
        }
        if (hintsLeft <= 0) {
            seeker.sendMessage(ChatColor.RED + "힌트를 모두 사용했습니다.");
            return;
        }

        hintsLeft--;
        broadcast(ChatColor.YELLOW + "⚡ " + seeker.getName() + " 이(가) 힌트를 사용했습니다! (남은 횟수: " + hintsLeft + ")");

        // 살아있는 모든 도망자 위치에 폭죽 발사
        for (UUID uid : hiders) {
            Player hider = Bukkit.getPlayer(uid);
            if (hider == null || hider.getGameMode() == GameMode.SPECTATOR) continue;

            Location loc = hider.getLocation().add(0, 1, 0);
            org.bukkit.entity.Firework fw = hider.getWorld().spawn(loc, org.bukkit.entity.Firework.class);
            org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
            org.bukkit.FireworkEffect effect = org.bukkit.FireworkEffect.builder()
                    .with(org.bukkit.FireworkEffect.Type.BURST)
                    .withColor(org.bukkit.Color.RED)
                    .withFade(org.bukkit.Color.ORANGE)
                    .trail(true)
                    .build();
            meta.addEffect(effect);
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        }
    }

    // ─────────────────────────────────────────────
    //  타이머 공통 로직
    // ─────────────────────────────────────────────

    private void startTimer(Runnable onEnd) {
        if (timerTask != null) timerTask.cancel();

        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    cancel();
                    onEnd.run();
                    return;
                }

                // 보스바 업데이트
                int totalTime = state == GameState.HIDING
                        ? plugin.getConfig().getInt("times.hide-time", 60)
                        : plugin.getConfig().getInt("times.game-time", 300);
                bossBar.setProgress(Math.max(0, (double) timeLeft / totalTime));

                String phase = state == GameState.HIDING ? ChatColor.GREEN + "숨기" : ChatColor.RED + "술래잡기";
                bossBar.setTitle(phase + " " + ChatColor.WHITE + formatTime(timeLeft));

                // 카운트다운 소리 (10초 이하)
                if (timeLeft <= 10) {
                    for (Player p : bossBar.getPlayers()) {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                    }
                }

                updateScoreboard();
                timeLeft--;
            }
        };
        timerTask.runTaskTimer(plugin, 0L, 20L);
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }

    // ─────────────────────────────────────────────
    //  스코어보드
    // ─────────────────────────────────────────────

    public void updateScoreboard() {
        ScoreboardManager sbm = Bukkit.getScoreboardManager();

        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard sb = sbm.getNewScoreboard();
            Objective obj = sb.registerNewObjective("bhs", "dummy",
                    ChatColor.YELLOW + "" + ChatColor.BOLD + "블럭 숨바꼭질");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);

            int line = 10;
            obj.getScore(ChatColor.WHITE + "─────────────").setScore(line--);
            obj.getScore(ChatColor.GRAY + "상태: " + ChatColor.WHITE + getStateName()).setScore(line--);
            obj.getScore(ChatColor.GRAY + "남은 시간: " + ChatColor.WHITE + formatTime(timeLeft)).setScore(line--);
            obj.getScore(ChatColor.WHITE + " ").setScore(line--);
            obj.getScore(ChatColor.GREEN + "도망자: " + ChatColor.WHITE + countAliveHiders() + "명").setScore(line--);
            obj.getScore(ChatColor.RED + "술래: " + ChatColor.WHITE + seekers.size() + "명").setScore(line--);
            obj.getScore(ChatColor.WHITE + "  ").setScore(line--);
            if (state == GameState.SEEKING) {
                obj.getScore(ChatColor.YELLOW + "힌트: " + ChatColor.WHITE + hintsLeft + "회").setScore(line--);
            }

            // 내 역할
            String role;
            if (hiders.contains(p.getUniqueId())) role = ChatColor.GREEN + "도망자";
            else if (seekers.contains(p.getUniqueId())) role = ChatColor.RED + "술래";
            else role = ChatColor.GRAY + "관전자";
            obj.getScore(ChatColor.GRAY + "내 역할: " + role).setScore(line--);

            p.setScoreboard(sb);
        }
    }

    private String getStateName() {
        return switch (state) {
            case WAITING -> ChatColor.GRAY + "대기 중";
            case HIDING -> ChatColor.GREEN + "숨기 중";
            case SEEKING -> ChatColor.RED + "술래잡기";
            case ENDED -> ChatColor.YELLOW + "종료";
        };
    }

    // ─────────────────────────────────────────────
    //  유틸
    // ─────────────────────────────────────────────

    private void broadcast(String msg) {
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    public GameState getState() { return state; }
    public Set<UUID> getHiders() { return hiders; }
    public Set<UUID> getSeekers() { return seekers; }

    public void setLobbySpawn(Location loc) { this.lobbySpawn = loc; }
    public void setHiderSpawn(Location loc) { this.hiderSpawn = loc; }
    public void setSeekerSpawn(Location loc) { this.seekerSpawn = loc; }

    public Location getLobbySpawn() { return lobbySpawn; }
    public Location getHiderSpawn() { return hiderSpawn; }
    public Location getSeekerSpawn() { return seekerSpawn; }

    public BossBar getBossBar() { return bossBar; }
}
