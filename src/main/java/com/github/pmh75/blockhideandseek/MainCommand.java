package com.github.pmh75.blockhideandseek;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {

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

            case "start" -> {
                if (!player.isOp()) {
                    noPermission(player);
                    return true;
                }
                plugin.getGameManager().startGame();
            }

            case "stop" -> {
                if (!player.isOp()) {
                    noPermission(player);
                    return true;
                }
                plugin.getGameManager().endGame(false);
                player.sendMessage(ChatColor.YELLOW + "게임을 강제 종료했습니다.");
            }

            case "setlobby" -> {
                if (!player.isOp()) {
                    noPermission(player);
                    return true;
                }
                plugin.getGameManager().setLobbySpawn(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "로비 스폰을 현재 위치로 설정했습니다.");
            }

            case "sethider" -> {
                if (!player.isOp()) {
                    noPermission(player);
                    return true;
                }
                plugin.getGameManager().setHiderSpawn(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "도망자 스폰을 현재 위치로 설정했습니다.");
            }

            case "setseeker" -> {
                if (!player.isOp()) {
                    noPermission(player);
                    return true;
                }
                plugin.getGameManager().setSeekerSpawn(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "술래 스폰을 현재 위치로 설정했습니다.");
            }

            case "setkit" -> {
                if (!player.isOp()) {
                    noPermission(player);
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "사용법: /bhs setkit [hider|seeker]");
                    return true;
                }
                plugin.getKitManager().openKitEditor(player, args[1]);
            }

            case "blocks" -> {
                if (!player.isOp()) {
                    noPermission(player);
                    return true;
                }
                plugin.getMode2BlockManageMenu().open(player);
            }

            case "mode" -> {
                if (!player.isOp()) {
                    noPermission(player);
                    return true;
                }
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
                if (!player.isOp()) {
                    noPermission(player);
                    return true;
                }
                plugin.reloadConfig();
                player.sendMessage(ChatColor.GREEN + "config.yml 을 리로드했습니다.");
            }

            case "hint" -> plugin.getGameManager().useHint(player);

            case "hunter" -> {
                if (!player.isOp()) {
                    noPermission(player);
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "사용법: /bhs hunter <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "플레이어를 찾을 수 없습니다: " + args[1]);
                    return true;
                }
                if (plugin.getGameManager().getSeekers().contains(target.getUniqueId())) {
                    player.sendMessage(ChatColor.YELLOW + target.getName() + " 은(는) 이미 술래입니다.");
                    return true;
                }
                if (plugin.getGameManager().setForceHunter(target)) {
                    player.sendMessage(ChatColor.GREEN + target.getName() + " 을(를) 술래로 설정했습니다.");
                } else {
                    player.sendMessage(ChatColor.RED + "술래 설정에 실패했습니다.");
                }
            }

            case "huntercount" -> {
                if (!player.isOp()) {
                    noPermission(player);
                    return true;
                }
                if (args.length < 2) {
                    int effective = plugin.getGameManager().getEffectiveHunterCount();
                    int configDefault = plugin.getGameManager().getConfigHunterCount();
                    player.sendMessage(ChatColor.YELLOW + "현재 술래 인원: " + effective + "명 (config 기본값: " + configDefault + "명)");
                    return true;
                }
                try {
                    int count = Integer.parseInt(args[1]);
                    if (count < 1) {
                        player.sendMessage(ChatColor.RED + "술래 인원은 1 이상이어야 합니다.");
                        return true;
                    }
                    if (!plugin.getConfig().getBoolean("game.allow-runtime-hunter-change", true)) {
                        player.sendMessage(ChatColor.RED + "런타임 술래 인원 변경이 비활성화되어 있습니다.");
                        return true;
                    }
                    if (plugin.getGameManager().setRuntimeHunterCount(count)) {
                        player.sendMessage(ChatColor.GREEN + "술래 인원을 " + count + "명으로 설정했습니다. (다음 게임 시작부터 적용)");
                    } else {
                        player.sendMessage(ChatColor.RED + "술래 인원 설정에 실패했습니다.");
                    }
                } catch (NumberFormatException ex) {
                    player.sendMessage(ChatColor.RED + "올바른 숫자를 입력하세요.");
                }
            }

            case "emote" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "사용법: /bhs emote <" + String.join("|", plugin.getEmoteManager().getRegistry().getIds()) + ">");
                    return true;
                }
                plugin.getEmoteManager().useEmote(player, args[1]);
            }

            case "skill" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "사용법: /bhs skill <" + String.join("|", plugin.getSkillManager().getRegistry().getIds()) + ">");
                    return true;
                }
                plugin.getSkillManager().useSkill(player, args[1]);
            }

            case "test" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "사용법: /bhs test <블럭이름|craftengine:id>");
                    return true;
                }

                DisguiseBlock disguise = parseTestDisguise(player, args[1]);
                if (disguise == null) {
                    player.sendMessage(ChatColor.RED + "잘못된 블럭 이름이거나 CraftEngine 블럭을 찾을 수 없습니다.");
                    return true;
                }

                if (disguise.isCustom()) {
                    plugin.getDisguiseManager().disguiseAsBlock(player, disguise);
                    player.sendMessage(ChatColor.GREEN + disguise.displayName() + " 커스텀 블럭으로 위장 테스트!");
                } else {
                    plugin.getDisguiseManager().disguise(player, disguise);
                    player.sendMessage(ChatColor.GREEN + disguise.displayName() + " 으로 위장 테스트! (Shift = 격자 고정)");
                }
            }

            case "untest" -> {
                plugin.getDisguiseManager().undisguise(player);
                player.sendMessage(ChatColor.GREEN + "위장 해제.");
            }

            default -> sendHelp(player);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("hint");
            options.add("emote");
            options.add("skill");
            if (sender.isOp()) {
                options.addAll(Arrays.asList("start", "stop", "setlobby", "sethider", "setseeker", "setkit", "blocks", "mode", "reload", "test", "untest", "hunter", "huntercount"));
            }
            return filter(options, args[0]);
        }

        if (args.length == 2 && sender.isOp()) {
            if (args[0].equalsIgnoreCase("setkit")) {
                return filter(Arrays.asList("hider", "seeker"), args[1]);
            }
            if (args[0].equalsIgnoreCase("mode")) {
                return filter(Arrays.asList("1", "2"), args[1]);
            }
            if (args[0].equalsIgnoreCase("test")) {
                return filter(testBlockSuggestions(), args[1]);
            }
            if (args[0].equalsIgnoreCase("hunter")) {
                List<String> names = new ArrayList<>();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    names.add(online.getName());
                }
                return filter(names, args[1]);
            }
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("emote")) {
                return filter(plugin.getEmoteManager().getRegistry().getIds(), args[1]);
            }
            if (args[0].equalsIgnoreCase("skill")) {
                return filter(plugin.getSkillManager().getRegistry().getIds(), args[1]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> blockMaterials() {
        List<String> materials = new ArrayList<>();
        for (Material mat : Material.values()) {
            if (mat.isBlock()) materials.add(mat.name());
        }
        return materials;
    }

    private List<String> testBlockSuggestions() {
        List<String> suggestions = new ArrayList<>();
        suggestions.addAll(plugin.getConfig().getStringList("selectable-blocks"));
        suggestions.addAll(plugin.getConfig().getStringList("craftengine.blocks"));
        if (suggestions.isEmpty()) {
            suggestions.addAll(blockMaterials());
        }
        return suggestions;
    }

    private DisguiseBlock parseTestDisguise(Player player, String rawId) {
        Material material = Material.matchMaterial(rawId.toUpperCase());
        if (material != null && material.isBlock()) {
            return DisguiseBlock.vanilla(material);
        }

        CraftEngineHook hook = plugin.getCraftEngineHook();
        if (hook == null || !hook.isAvailable()) {
            return null;
        }

        return hook.createBlock(rawId, player);
    }

    private List<String> filter(List<String> list, String prefix) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(s);
            }
        }
        return result;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "══ 블럭 숨바꼭질 명령어 ══");
        if (player.isOp()) {
            sendClickableCommand(player, "/bhs start", "게임 시작");
            sendClickableCommand(player, "/bhs stop", "게임 종료");
            sendClickableCommand(player, "/bhs setlobby", "로비 스폰 설정");
            sendClickableCommand(player, "/bhs sethider", "도망자 스폰 설정");
            sendClickableCommand(player, "/bhs setseeker", "술래 스폰 설정");
            sendClickableCommand(player, "/bhs setkit hider", "도망자 키트 설정 GUI");
            sendClickableCommand(player, "/bhs setkit seeker", "술래 키트 설정 GUI");
            sendClickableCommand(player, "/bhs blocks", "모드2 선택 가능 블럭 관리 GUI");
            sendClickableCommand(player, "/bhs mode 1", "모드 1로 변경");
            sendClickableCommand(player, "/bhs mode 2", "모드 2로 변경");
            sendClickableCommand(player, "/bhs reload", "설정 리로드");
            sendClickableCommand(player, "/bhs hunter <player>", "술래 강제 지정");
            sendClickableCommand(player, "/bhs huntercount <count>", "술래 인원 설정");
        }
        sendClickableCommand(player, "/bhs hint", "힌트 사용 (술래 전용)");
        sendClickableCommand(player, "/bhs emote <type>", "도발 (도망자 전용)");
        sendClickableCommand(player, "/bhs skill <type>", "스킬 사용");
    }

    private void sendClickableCommand(Player player, String command, String description) {
        TextComponent cmdComponent = new TextComponent(command);
        cmdComponent.setColor(net.md_5.bungee.api.ChatColor.GOLD);
        cmdComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        cmdComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("클릭하여 명령어 입력")));

        TextComponent descComponent = new TextComponent(" - " + description);
        descComponent.setColor(net.md_5.bungee.api.ChatColor.WHITE);

        cmdComponent.addExtra(descComponent);
        player.spigot().sendMessage(cmdComponent);
    }

    private void noPermission(Player player) {
        player.sendMessage(ChatColor.RED + "관리자만 사용할 수 있습니다.");
    }
}
