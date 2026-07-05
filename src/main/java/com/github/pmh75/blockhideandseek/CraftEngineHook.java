package com.github.pmh75.blockhideandseek;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class CraftEngineHook {

    private final BlockHideAndSeek plugin;
    private final Set<String> warnedIds = new HashSet<>();

    public CraftEngineHook(BlockHideAndSeek plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("CraftEngine");
    }

    public DisguiseBlock createBlock(String configuredId, Player player) {
        if (!isAvailable()) {
            warnOnce("unavailable", "CraftEngine 플러그인이 없어 커스텀 블럭을 불러올 수 없습니다.");
            return null;
        }

        String id = configuredId == null ? "" : configuredId.trim();
        if (id.isEmpty()) {
            return null;
        }

        try {
            Key key = Key.withDefaultNamespace(id, defaultNamespace());
            BlockDefinition block = CraftEngineBlocks.byId(key);
            if (block == null) {
                warnOnce("missing:" + key.asString(), "CraftEngine 블럭을 찾을 수 없습니다: " + key.asString());
                return null;
            }

            BlockData blockData = CraftEngineBlocks.getBukkitBlockData(block.defaultState());
            ItemStack icon = createIcon(key, player, blockData.getMaterial());
            return DisguiseBlock.custom(key.asString(), blockData, icon);
        } catch (RuntimeException | LinkageError ex) {
            warnOnce("error:" + id, "CraftEngine 블럭 로드 실패: " + id + " (" + ex.getClass().getSimpleName() + ")");
            return null;
        }
    }

    public String getCustomItemId(ItemStack item) {
        if (!isAvailable() || item == null || item.getType() == Material.AIR) {
            return null;
        }

        try {
            if (!CraftEngineItems.isCustomItem(item)) {
                return null;
            }
            Key key = CraftEngineItems.getCustomItemId(item);
            return key == null ? null : key.asString();
        } catch (RuntimeException | LinkageError ex) {
            warnOnce("item-id", "CraftEngine 아이템 ID 조회 실패: " + ex.getClass().getSimpleName());
            return null;
        }
    }

    public String getCustomBlockId(Block block) {
        if (!isAvailable() || block == null) {
            return null;
        }

        try {
            if (!CraftEngineBlocks.isCustomBlock(block)) {
                return null;
            }
            ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(block);
            if (state == null || state.owner() == null || state.owner().value() == null) {
                return null;
            }
            return state.owner().value().id().asString();
        } catch (RuntimeException | LinkageError ex) {
            warnOnce("block-id", "CraftEngine 블럭 ID 조회 실패: " + ex.getClass().getSimpleName());
            return null;
        }
    }

    private String defaultNamespace() {
        String namespace = plugin.getConfig().getString("craftengine.default-namespace", "craftengine");
        return namespace == null || namespace.isBlank() ? "craftengine" : namespace.trim();
    }

    private ItemStack createIcon(Key key, Player player, Material fallback) {
        BukkitItemDefinition item = CraftEngineItems.byId(key.asString());
        if (item == null) {
            item = CraftEngineItems.byId(key.value());
        }

        if (item != null) {
            try {
                return player == null ? item.buildBukkitItem() : item.buildBukkitItem(player);
            } catch (RuntimeException | LinkageError ex) {
                warnOnce("icon:" + key.asString(), "CraftEngine 아이콘 생성 실패: " + key.asString());
            }
        }

        Material iconMaterial = fallback.isItem() ? fallback : Material.BARRIER;
        return new ItemStack(iconMaterial);
    }

    private void warnOnce(String key, String message) {
        if (warnedIds.add(key)) {
            plugin.getLogger().warning(message);
        }
    }
}
