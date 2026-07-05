package com.github.pmh75.blockhideandseek;

import org.bukkit.plugin.java.JavaPlugin;

public class BlockHideAndSeek extends JavaPlugin {

    private DisguiseManager disguiseManager;
    private KitManager kitManager;
    private GameManager gameManager;
    private BlockSelectMenu blockSelectMenu;
    private Mode2BlockManageMenu mode2BlockManageMenu;
    private CraftEngineHook craftEngineHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        craftEngineHook = createCraftEngineHook();

        disguiseManager = new DisguiseManager(this);
        kitManager = new KitManager(this);
        gameManager = new GameManager(this);
        blockSelectMenu = new BlockSelectMenu(this);
        mode2BlockManageMenu = new Mode2BlockManageMenu(this);

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
    public Mode2BlockManageMenu getMode2BlockManageMenu() { return mode2BlockManageMenu; }
    public CraftEngineHook getCraftEngineHook() { return craftEngineHook; }

    private CraftEngineHook createCraftEngineHook() {
        if (!getServer().getPluginManager().isPluginEnabled("CraftEngine")) {
            return null;
        }

        try {
            getLogger().info("CraftEngine detected. Custom block disguise support enabled.");
            return new CraftEngineHook(this);
        } catch (LinkageError error) {
            getLogger().warning("CraftEngine API를 불러오지 못해 커스텀 블럭 지원을 비활성화합니다: " + error.getClass().getSimpleName());
            return null;
        }
    }
}
