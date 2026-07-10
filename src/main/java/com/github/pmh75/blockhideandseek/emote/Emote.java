package com.github.pmh75.blockhideandseek.emote;

import org.bukkit.entity.Player;

public interface Emote {

    String id();

    String displayName();

    void play(Player player);
}
