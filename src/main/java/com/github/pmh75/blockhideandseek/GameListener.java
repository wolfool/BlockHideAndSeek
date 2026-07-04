package com.github.pmh75.blockhideandseek;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class GameListener implements Listener {

    private final BlockHideAndSeek plugin;

    public GameListener(BlockHideAndSeek plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────
    //  위장 - 웅크리기 고정
    // ─────────────────────────────────────────────

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        boolean success = plugin.getDisguiseManager().toggleSolidify(player, event.isSneaking());

        // 모드 1에서 발 아래 블럭 없어 고정 실패한 경우
        if (event.isSneaking() && !success) {
            player.sendActionBar(net.kyori.adventure.text.Component.text(
                    "§c⚠ 발 아래 블럭이 없어 고정할 수 없습니다!"));
        }
    }

    // ─────────────────────────────────────────────
    //  질식 데미지 방지 (블럭 속에 숨을 때)
    // ─────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
            if (plugin.getDisguiseManager().isDisguised(player)) {
                event.setCancelled(true);
            }
        }
    }

    // ─────────────────────────────────────────────
    //  도망자 사망 처리 → 관전자 전환
    // ─────────────────────────────────────────────

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GameManager gm = plugin.getGameManager();
        if (gm.getState() != GameManager.GameState.SEEKING
                && gm.getState() != GameManager.GameState.HIDING) return;

        if (gm.getHiders().contains(player.getUniqueId())) {
            event.setCancelled(true); // 실제 사망 이벤트 취소
            player.setHealth(player.getMaxHealth()); // 체력 회복 후
            gm.onHiderDie(player); // 관전자 처리
        }
    }

    // ─────────────────────────────────────────────
    //  맵 보호 - 블럭 파괴/설치 방지
    // ─────────────────────────────────────────────

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        GameManager.GameState state = plugin.getGameManager().getState();
        if (state == GameManager.GameState.HIDING || state == GameManager.GameState.SEEKING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        GameManager.GameState state = plugin.getGameManager().getState();
        if (state == GameManager.GameState.HIDING || state == GameManager.GameState.SEEKING) {
            event.setCancelled(true);
        }
    }

    // ─────────────────────────────────────────────
    //  아이템 드롭 방지
    // ─────────────────────────────────────────────

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        GameManager.GameState state = plugin.getGameManager().getState();
        if (state == GameManager.GameState.HIDING || state == GameManager.GameState.SEEKING) {
            event.setCancelled(true);
        }
    }

    // ─────────────────────────────────────────────
    //  탈주자 처리
    // ─────────────────────────────────────────────

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        if (gm.getState() != GameManager.GameState.WAITING) {
            if (gm.getHiders().contains(player.getUniqueId())) {
                plugin.getDisguiseManager().undisguise(player);
                gm.onHiderDie(player);
            }
        }
        plugin.getDisguiseManager().undisguise(player);
    }
}
