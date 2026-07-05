package com.github.pmh75.blockhideandseek;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

public final class DisguiseBlock {

    private final String id;
    private final String displayName;
    private final Material material;
    private final BlockData blockData;
    private final ItemStack icon;
    private final boolean custom;

    private DisguiseBlock(String id, String displayName, Material material, BlockData blockData, ItemStack icon, boolean custom) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.blockData = blockData;
        this.icon = sanitizeIcon(icon, material);
        this.custom = custom;
    }

    public static DisguiseBlock vanilla(Material material) {
        return new DisguiseBlock(
                material.name(),
                material.name(),
                material,
                Bukkit.createBlockData(material),
                new ItemStack(material.isItem() ? material : Material.BARRIER),
                false
        );
    }

    public static DisguiseBlock custom(String id, BlockData blockData, ItemStack icon) {
        return new DisguiseBlock(
                id,
                id,
                blockData.getMaterial(),
                blockData,
                icon,
                true
        );
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Material material() {
        return material;
    }

    public boolean isCustom() {
        return custom;
    }

    public BlockData createBlockData() {
        return blockData.clone();
    }

    public ItemStack createIcon() {
        ItemStack copy = icon.clone();
        copy.setAmount(1);
        return copy;
    }

    public ItemStack createHelmetItem() {
        return createIcon();
    }

    private static ItemStack sanitizeIcon(ItemStack item, Material fallback) {
        if (item != null && item.getType() != Material.AIR) {
            ItemStack copy = item.clone();
            copy.setAmount(1);
            return copy;
        }

        Material iconMaterial = fallback.isItem() ? fallback : Material.BARRIER;
        return new ItemStack(iconMaterial);
    }
}
