package com.github.pmh75.blockhideandseek;

import org.bukkit.plugin.java.JavaPlugin;

public class BlockHideAndSeek extends JavaPlugin {

    private DisguiseManager disguiseManager;
    private KitManager kitManager;
    private GameManager gameManager;
    private BlockSelectMenu blockSelectMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        disguiseManager = new DisguiseManager(this);
        kitManager = new KitManager(this);
        gameManager = new GameManager(this);
        blockSelectMenu = new BlockSelectMenu(this);

        getCommand("bhs").setExecutor(new MainCommand(this));
        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        getLogger().info("Block Hide and Seek v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (gameManager.getState() != GameManager.GameState.WAITING) {
            gameManager.endGame(false);
        }
        disguiseManager.cleanupAll();
        getLogger().info("Block Hide and Seek disabled!");
    }

    public DisguiseManager getDisguiseManager() { return disguiseManager; }
    public KitManager getKitManager() { return kitManager; }
    public GameManager getGameManager() { return gameManager; }
    public BlockSelectMenu getBlockSelectMenu() { return blockSelectMenu; }
}
