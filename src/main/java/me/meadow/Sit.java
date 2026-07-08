package me.meadow;

import me.meadow.command.PlayerSitCommand;
import me.meadow.command.PoseCommand;
import me.meadow.placeholder.SitPlaceholderExpansion;
import me.meadow.pose.PlayerSitSettings;
import me.meadow.pose.PoseListener;
import me.meadow.pose.PoseManager;
import me.meadow.pose.PoseType;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Sit extends JavaPlugin {
    public static final String PLUGIN_TAG = "SitManager";

    private PoseManager poses;
    private PlayerSitSettings playerSitSettings;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getConfig().addDefault("features.sit.enabled", true);
        getConfig().addDefault("features.lay.enabled", true);
        getConfig().addDefault("features.spin.enabled", true);
        getConfig().addDefault("features.crawl.enabled", true);
        getConfig().addDefault("features.player-sit.enabled", true);

        getConfig().addDefault("features.sit.get-up-hint", "ꜱɴᴇᴀᴋ ᴛᴏ ꜱᴛᴀɴᴅ ᴜᴘ");
        getConfig().addDefault("features.lay.get-up-hint", "ꜱɴᴇᴀᴋ ᴛᴏ ɢᴇᴛ ᴜᴘ");
        getConfig().addDefault("features.spin.get-up-hint", "ꜱɴᴇᴀᴋ ᴛᴏ ꜱᴛᴏᴘ ꜱᴘɪɴɴɪɴɢ");
        getConfig().addDefault("features.crawl.get-up-hint", "ꜱɴᴇᴀᴋ ᴛᴏ ꜱᴛᴏᴘ ᴄʀᴀᴡʟɪɴɢ");
        getConfig().addDefault("features.player-sit.get-up-hint", "ꜱɴᴇᴀᴋ ᴛᴏ ɢᴇᴛ ᴅᴏᴡɴ");

        getConfig().addDefault("lay-snoring-sound", true);

        getConfig().addDefault("messages.no-permission", "§a→ §fYou do not have permission to use this.");
        getConfig().addDefault("messages.feature-disabled", "§a→ §fThis feature is currently disabled.");
        getConfig().addDefault("messages.player-sit-enabled", "§a→ §fPlayer sit is now §aenabled§f.");
        getConfig().addDefault("messages.player-sit-disabled", "§a→ §fPlayer sit is now §cdisabled§f.");
        getConfig().addDefault("messages.reload", "§a→ §fSIT configuration reloaded.");
        getConfig().options().copyDefaults(true);
        saveConfig();

        this.playerSitSettings = new PlayerSitSettings(this);
        this.poses = new PoseManager(this);

        getServer().getPluginManager().registerEvents(new PoseListener(this, poses), this);

        registerPose("sit", PoseType.SIT);
        registerPose("lay", PoseType.LAY);
        registerPose("spin", PoseType.SPIN);
        registerPose("crawl", PoseType.CRAWL);
        registerPlayerSitCommand();

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new SitPlaceholderExpansion(this).register();
        }
    }

    @Override
    public void onDisable() {
        if (poses != null) {
            poses.shutdown();
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        if (playerSitSettings != null) {
            playerSitSettings.reload();
        }
    }

    public PoseManager poses() {
        return poses;
    }

    public PlayerSitSettings playerSitSettings() {
        return playerSitSettings;
    }

    public boolean laySnoringSound() {
        return getConfig().getBoolean("lay-snoring-sound", true);
    }

    public boolean featureEnabled(PoseType type) {
        return switch (type) {
            case SIT -> getConfig().getBoolean("features.sit.enabled", true);
            case LAY -> getConfig().getBoolean("features.lay.enabled", true);
            case SPIN -> getConfig().getBoolean("features.spin.enabled", true);
            case CRAWL -> getConfig().getBoolean("features.crawl.enabled", true);
            case HEAD -> getConfig().getBoolean("features.player-sit.enabled", true);
        };
    }

    public String getUpHint(PoseType type) {
        String path = switch (type) {
            case SIT -> "features.sit.get-up-hint";
            case LAY -> "features.lay.get-up-hint";
            case SPIN -> "features.spin.get-up-hint";
            case CRAWL -> "features.crawl.get-up-hint";
            case HEAD -> "features.player-sit.get-up-hint";
        };

        return getConfig().getString(path, "");
    }

    public String message(String path) {
        return getConfig().getString("messages." + path, "");
    }

    private void registerPlayerSitCommand() {
        PluginCommand command = getCommand("playersit");
        if (command == null) {
            getLogger().warning("Command missing from plugin.yml: playersit");
            return;
        }

        PlayerSitCommand executor = new PlayerSitCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerPose(String name, PoseType type) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command missing from plugin.yml: " + name);
            return;
        }

        PoseCommand executor = new PoseCommand(this, poses, type);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
