package com.github.pmh75.blockhideandseek;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
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
    //  위장 셜커(히트박스) 피격 시 플레이어에게 데미지 전달
    // ─────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onShulkerDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Shulker) {
            Player owner = plugin.getDisguiseManager().getOwnerOfHitbox(event.getEntity());
            if (owner != null) {
                event.setCancelled(true);
                if (event.getDamager() instanceof Player attacker) {
                    owner.damage(event.getDamage(), attacker);
                }
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
    //  아이템 드롭 및 인벤토리 헬멧 클릭 방지
    // ─────────────────────────────────────────────

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        GameManager.GameState state = plugin.getGameManager().getState();
        if (state == GameManager.GameState.HIDING || state == GameManager.GameState.SEEKING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (plugin.getDisguiseManager().isDisguised(player)) {
            // 헬멧 슬롯(갑옷) 클릭 및 숫자키 스왑, 버리기 등 방지
            if (event.getSlotType() == InventoryType.SlotType.ARMOR || event.getRawSlot() == 5) {
                event.setCancelled(true);
                return;
            }
            // 쉬프트 클릭을 통해 아이템이 헬멧 칸으로 들어가는 것 방지
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
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
