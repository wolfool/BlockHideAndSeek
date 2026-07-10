package com.github.pmh75.blockhideandseek.skill;

import org.bukkit.entity.Player;

public interface Skill {

    String id();

    String displayName();

    boolean isForHunter();

    void activate(Player player);
}
