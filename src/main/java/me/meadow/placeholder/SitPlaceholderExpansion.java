package me.meadow.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.meadow.Sit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class SitPlaceholderExpansion extends PlaceholderExpansion {
    private final Sit plugin;

    public SitPlaceholderExpansion(Sit plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "sit";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MeadowStudios";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || player.getUniqueId() == null) {
            return "";
        }

        if (params.equalsIgnoreCase("playersit") || params.equalsIgnoreCase("player_sit")) {
            return Boolean.toString(plugin.playerSitSettings().enabled(player.getUniqueId()));
        }

        return null;
    }
}
