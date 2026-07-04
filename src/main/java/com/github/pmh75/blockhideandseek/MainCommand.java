package com.github.pmh75.blockhideandseek;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MainCommand implements CommandExecutor {

    private final BlockHideAndSeek plugin;

    public MainCommand(BlockHideAndSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // ── 관리자 명령어 ──────────────────────────────────

            case "start" -> {
                if (!player.isOp()) { noPermission(player); return true; }
                plugin.getGameManager().startGame();
            }

            case "stop" -> {
                if (!player.isOp()) { noPermission(player); return true; }
                plugin.getGameManager().endGame(false);
                player.sendMessage(ChatColor.YELLOW + "게임을 강제 종료했습니다.");
            }

            case "setlobby" -> {
                if (!player.isOp()) { noPermission(player); return true; }
                plugin.getGameManager().setLobbySpawn(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "로비 스폰을 현재 위치로 설정했습니다.");
            }

            case "sethider" -> {
                if (!player.isOp()) { noPermission(player); return true; }
                plugin.getGameManager().setHiderSpawn(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "도망자 스폰을 현재 위치로 설정했습니다.");
            }

            case "setseeker" -> {
                if (!player.isOp()) { noPermission(player); return true; }
                plugin.getGameManager().setSeekerSpawn(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "술래 스폰을 현재 위치로 설정했습니다.");
            }

            case "setkit" -> {
                if (!player.isOp()) { noPermission(player); return true; }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "사용법: /bhs setkit [hider|seeker]");
                    return true;
                }
                plugin.getKitManager().openKitEditor(player, args[1]);
            }

            case "mode" -> {
                if (!player.isOp()) { noPermission(player); return true; }
                if (args.length < 2 || (!args[1].equals("1") && !args[1].equals("2"))) {
                    player.sendMessage(ChatColor.RED + "사용법: /bhs mode [1|2]");
                    player.sendMessage(ChatColor.GRAY + "  1 = 쉬프트 시 발 아래 블럭 고정");
                    player.sendMessage(ChatColor.GRAY + "  2 = 주기적 블럭 변경 선택형");
                    return true;
                }
                plugin.getConfig().set("game-mode", Integer.parseInt(args[1]));
                plugin.saveConfig();
                player.sendMessage(ChatColor.GREEN + "게임 모드를 " + args[1] + "번으로 변경했습니다.");
            }

            case "reload" -> {
                if (!player.isOp()) { noPermission(player); return true; }
                plugin.reloadConfig();
                player.sendMessage(ChatColor.GREEN + "config.yml 을 리로드했습니다.");
            }

            // ── 술래 힌트 ──────────────────────────────────────

            case "hint" -> plugin.getGameManager().useHint(player);

            // ── 위장 테스트 명령어 (개발용) ───────────────────────

            case "test" -> {
                org.bukkit.Material mat = org.bukkit.Material.OAK_LOG;
                if (args.length > 1) {
                    try {
                        mat = org.bukkit.Material.valueOf(args[1].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(ChatColor.RED + "잘못된 블럭 이름입니다.");
                        return true;
                    }
                }
                plugin.getDisguiseManager().disguise(player, mat);
                player.sendMessage(ChatColor.GREEN + mat.name() + " 으로 위장 테스트! (Shift = 격자 고정)");
            }

            case "untest" -> {
                plugin.getDisguiseManager().undisguise(player);
                player.sendMessage(ChatColor.GREEN + "위장 해제.");
            }

            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "══ 블럭 숨바꼭질 명령어 ══");
        if (player.isOp()) {
            player.sendMessage(ChatColor.GOLD + "/bhs start|stop" + ChatColor.WHITE + " - 게임 시작/종료");
            player.sendMessage(ChatColor.GOLD + "/bhs setlobby|sethider|setseeker" + ChatColor.WHITE + " - 스폰 설정");
            player.sendMessage(ChatColor.GOLD + "/bhs setkit [hider|seeker]" + ChatColor.WHITE + " - 키트 설정 GUI");
            player.sendMessage(ChatColor.GOLD + "/bhs reload" + ChatColor.WHITE + " - 설정 리로드");
        }
        player.sendMessage(ChatColor.GREEN + "/bhs hint" + ChatColor.WHITE + " - 힌트 사용 (술래 전용)");
    }

    private void noPermission(Player player) {
        player.sendMessage(ChatColor.RED + "관리자만 사용할 수 있습니다.");
    }
}
