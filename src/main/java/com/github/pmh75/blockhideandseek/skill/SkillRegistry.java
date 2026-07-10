package com.github.pmh75.blockhideandseek.skill;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SkillRegistry {

    private final Map<String, Skill> skills = new LinkedHashMap<>();

    public SkillRegistry() {
        registerDefaults();
    }

    public void register(Skill skill) {
        skills.put(skill.id().toLowerCase(), skill);
    }

    public Skill get(String id) {
        if (id == null) {
            return null;
        }
        return skills.get(id.toLowerCase());
    }

    public List<Skill> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(skills.values()));
    }

    public List<String> getIds() {
        return new ArrayList<>(skills.keySet());
    }

    private void registerDefaults() {
        register(new Skill() {
            @Override
            public String id() {
                return "speed_boost";
            }

            @Override
            public String displayName() {
                return "술래 가속";
            }

            @Override
            public boolean isForHunter() {
                return true;
            }

            @Override
            public void activate(Player player) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 5, 1, false, true));
                player.sendMessage(ChatColor.RED + "술래 스킬: 5초간 가속!");
            }
        });

        register(new Skill() {
            @Override
            public String id() {
                return "invisibility_pulse";
            }

            @Override
            public String displayName() {
                return "잠깐 숨기";
            }

            @Override
            public boolean isForHunter() {
                return false;
            }

            @Override
            public void activate(Player player) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 3, 0, false, false));
                player.sendMessage(ChatColor.GREEN + "도망자 스킬: 3초간 투명!");
            }
        });
    }
}
