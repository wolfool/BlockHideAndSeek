package com.github.pmh75.blockhideandseek.emote;

import com.github.pmh75.blockhideandseek.BlockHideAndSeek;
import com.github.pmh75.blockhideandseek.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EmoteManager {

    private final BlockHideAndSeek plugin;
    private final EmoteRegistry registry;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public EmoteManager(BlockHideAndSeek plugin, EmoteRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public EmoteRegistry getRegistry() {
        return registry;
    }

    public boolean useEmote(Player player, String emoteId) {
        GameManager gm = plugin.getGameManager();
        if (gm.getState() != GameManager.GameState.HIDING
                && gm.getState() != GameManager.GameState.SEEKING) {
            player.sendMessage(ChatColor.RED + "게임 중에만 도발을 사용할 수 있습니다.");
            return false;
        }

        if (!gm.getHiders().contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "숨는 플레이어만 도발을 사용할 수 있습니다.");
            return false;
        }

        Emote emote = registry.get(emoteId);
        if (emote == null) {
            player.sendMessage(ChatColor.RED + "알 수 없는 도발입니다. 사용 가능: " + String.join(", ", registry.getIds()));
            return false;
        }

        long cooldownMs = plugin.getConfig().getInt("emote.cooldown-seconds", 30) * 1000L;
        long now = System.currentTimeMillis();
        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse != null && now - lastUse < cooldownMs) {
            long remain = (cooldownMs - (now - lastUse)) / 1000L;
            player.sendMessage(ChatColor.RED + "도발 쿨타임: " + remain + "초 남음");
            return false;
        }

        emote.play(player);
        cooldowns.put(player.getUniqueId(), now);
        plugin.getGameStatsManager().recordEmote(player);
        return true;
    }

    public void clearCooldowns() {
        cooldowns.clear();
    }
}
