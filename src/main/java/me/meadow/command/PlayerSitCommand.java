package me.meadow.command;

import java.util.Collections;
import java.util.List;

import me.meadow.Sit;
import me.meadow.pose.PoseType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PlayerSitCommand implements CommandExecutor, TabCompleter {
    private final Sit plugin;

    public PlayerSitCommand(Sit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (!plugin.featureEnabled(PoseType.HEAD)) {
            player.sendMessage(plugin.message("feature-disabled"));
            return true;
        }

        if (!player.hasPermission("sit.playersit")) {
            player.sendMessage(plugin.message("no-permission"));
            return true;
        }

        boolean enabled = plugin.playerSitSettings().toggle(player);

        if (!enabled) {
            plugin.poses().stopHeadRiders(player);

            if (plugin.poses().isInPose(player, PoseType.HEAD)) {
                plugin.poses().stop(player);
            }
        }

        player.sendMessage(plugin.message(enabled ? "player-sit-enabled" : "player-sit-disabled"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return Collections.emptyList();
    }
}
