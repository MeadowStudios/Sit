package me.meadow.pose;

import me.meadow.Sit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PlayerSitSettings {
    private static final String DISABLED_PATH = "player-sit-disabled";

    private final Sit plugin;
    private final File file;
    private FileConfiguration config;
    private final Set<UUID> disabled = new HashSet<>();

    public PlayerSitSettings(Sit plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        reload();
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);

        disabled.clear();

        for (String raw : config.getStringList(DISABLED_PATH)) {
            try {
                disabled.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public boolean enabled(Player player) {
        return player != null && enabled(player.getUniqueId());
    }

    public boolean enabled(UUID uuid) {
        return uuid != null && !disabled.contains(uuid);
    }

    public boolean toggle(Player player) {
        UUID uuid = player.getUniqueId();

        boolean nowEnabled;
        if (disabled.remove(uuid)) {
            nowEnabled = true;
        } else {
            disabled.add(uuid);
            nowEnabled = false;
        }

        save();
        return nowEnabled;
    }

    private void save() {
        List<String> values = new ArrayList<>();

        for (UUID uuid : disabled) {
            values.add(uuid.toString());
        }

        values.sort(String::compareTo);
        config.set(DISABLED_PATH, values);

        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder.");
                return;
            }

            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save player sit data: " + exception.getMessage());
        }
    }
}
