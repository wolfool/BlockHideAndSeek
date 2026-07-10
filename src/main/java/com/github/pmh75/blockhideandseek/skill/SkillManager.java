package com.github.pmh75.blockhideandseek.skill;

import com.github.pmh75.blockhideandseek.BlockHideAndSeek;
import com.github.pmh75.blockhideandseek.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class SkillManager {

    private final BlockHideAndSeek plugin;
    private final SkillRegistry registry;

    public SkillManager(BlockHideAndSeek plugin, SkillRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public SkillRegistry getRegistry() {
        return registry;
    }

    public boolean useSkill(Player player, String skillId) {
        GameManager gm = plugin.getGameManager();
        if (gm.getState() != GameManager.GameState.SEEKING) {
            player.sendMessage(ChatColor.RED + "술래잡기 중에만 스킬을 사용할 수 있습니다.");
            return false;
        }

        Skill skill = registry.get(skillId);
        if (skill == null) {
            player.sendMessage(ChatColor.RED + "알 수 없는 스킬입니다.");
            return false;
        }

        boolean isSeeker = gm.getSeekers().contains(player.getUniqueId());
        boolean isHider = gm.getHiders().contains(player.getUniqueId());

        if (skill.isForHunter()) {
            if (!plugin.getConfig().getBoolean("skill.hunter-enabled", true)) {
                player.sendMessage(ChatColor.RED + "술래 스킬이 비활성화되어 있습니다.");
                return false;
            }
            if (!isSeeker) {
                player.sendMessage(ChatColor.RED + "술래만 이 스킬을 사용할 수 있습니다.");
                return false;
            }
        } else {
            if (!plugin.getConfig().getBoolean("skill.hider-enabled", true)) {
                player.sendMessage(ChatColor.RED + "도망자 스킬이 비활성화되어 있습니다.");
                return false;
            }
            if (!isHider) {
                player.sendMessage(ChatColor.RED + "도망자만 이 스킬을 사용할 수 있습니다.");
                return false;
            }
        }

        skill.activate(player);
        return true;
    }
}
