package com.github.pmh75.blockhideandseek.emote;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmoteRegistry {

    private final Map<String, Emote> emotes = new LinkedHashMap<>();

    public EmoteRegistry() {
        registerDefaults();
    }

    public void register(Emote emote) {
        emotes.put(emote.id().toLowerCase(), emote);
    }

    public Emote get(String id) {
        if (id == null) {
            return null;
        }
        return emotes.get(id.toLowerCase());
    }

    public List<Emote> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(emotes.values()));
    }

    public List<String> getIds() {
        return new ArrayList<>(emotes.keySet());
    }

    private void registerDefaults() {
        register(new Emote() {
            @Override
            public String id() {
                return "chicken";
            }

            @Override
            public String displayName() {
                return "닭 울음";
            }

            @Override
            public void play(Player player) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, 1f, 1f);
            }
        });

        register(new Emote() {
            @Override
            public String id() {
                return "clap";
            }

            @Override
            public String displayName() {
                return "박수";
            }

            @Override
            public void play(Player player) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.2f);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 0.9f);
            }
        });

        register(new Emote() {
            @Override
            public String id() {
                return "here";
            }

            @Override
            public String displayName() {
                return "여기다~!";
            }

            @Override
            public void play(Player player) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1.5f);
                player.sendMessage("§e§l여기다~!");
            }
        });

        register(new Emote() {
            @Override
            public String id() {
                return "particle";
            }

            @Override
            public String displayName() {
                return "파티클";
            }

            @Override
            public void play(Player player) {
                player.getWorld().spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        player.getLocation().add(0, 1, 0),
                        20,
                        0.5, 0.5, 0.5,
                        0.05
                );
            }
        });
    }
}
