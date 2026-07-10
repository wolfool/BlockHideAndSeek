package com.github.pmh75.blockhideandseek;

import com.github.pmh75.blockhideandseek.emote.EmoteManager;
import com.github.pmh75.blockhideandseek.emote.EmoteRegistry;
import com.github.pmh75.blockhideandseek.record.GameRecordContext;
import com.github.pmh75.blockhideandseek.record.GameRecordRegistry;
import com.github.pmh75.blockhideandseek.skill.SkillManager;
import com.github.pmh75.blockhideandseek.skill.SkillRegistry;
import com.github.pmh75.blockhideandseek.stats.GameStatsManager;
import com.github.pmh75.blockhideandseek.system.ProximityWarningSystem;
import org.bukkit.plugin.java.JavaPlugin;

public class BlockHideAndSeek extends JavaPlugin {

    private DisguiseManager disguiseManager;
    private KitManager kitManager;
    private GameManager gameManager;
    private BlockSelectMenu blockSelectMenu;
    private Mode2BlockManageMenu mode2BlockManageMenu;
    private CraftEngineHook craftEngineHook;
    private GameStatsManager gameStatsManager;
    private GameRecordRegistry gameRecordRegistry;
    private EmoteManager emoteManager;
    private SkillManager skillManager;
    private ProximityWarningSystem proximityWarningSystem;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        craftEngineHook = createCraftEngineHook();

        disguiseManager = new DisguiseManager(this);
        kitManager = new KitManager(this);
        gameStatsManager = new GameStatsManager(this);
        gameRecordRegistry = new GameRecordRegistry();
        emoteManager = new EmoteManager(this, new EmoteRegistry());
        skillManager = new SkillManager(this, new SkillRegistry());
        proximityWarningSystem = new ProximityWarningSystem(this);
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
        proximityWarningSystem.stop();
        disguiseManager.cleanupAll();
        getLogger().info("Block Hide and Seek disabled!");
    }

    public DisguiseManager getDisguiseManager() { return disguiseManager; }
    public KitManager getKitManager() { return kitManager; }
    public GameManager getGameManager() { return gameManager; }
    public BlockSelectMenu getBlockSelectMenu() { return blockSelectMenu; }
    public Mode2BlockManageMenu getMode2BlockManageMenu() { return mode2BlockManageMenu; }
    public CraftEngineHook getCraftEngineHook() { return craftEngineHook; }
    public GameStatsManager getGameStatsManager() { return gameStatsManager; }
    public GameRecordRegistry getGameRecordRegistry() { return gameRecordRegistry; }
    public EmoteManager getEmoteManager() { return emoteManager; }
    public SkillManager getSkillManager() { return skillManager; }
    public ProximityWarningSystem getProximityWarningSystem() { return proximityWarningSystem; }

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
