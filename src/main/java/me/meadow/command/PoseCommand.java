package me.meadow.command;

import java.util.Collections;
import java.util.List;

import me.meadow.Sit;
import me.meadow.pose.PoseManager;
import me.meadow.pose.PoseType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PoseCommand implements CommandExecutor, TabCompleter {
    private final Sit plugin;
    private final PoseManager poses;
    private final PoseType type;

    public PoseCommand(Sit plugin, PoseManager poses, PoseType type) {
        this.plugin = plugin;
        this.poses = poses;
        this.type = type;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (type == PoseType.SIT && args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("sit.reload")) {
                sender.sendMessage(plugin.message("no-permission"));
                return true;
            }

            plugin.reloadPluginConfig();
            sender.sendMessage(plugin.message("reload"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            return true;
        }

        if (!plugin.featureEnabled(type)) {
            player.sendMessage(plugin.message("feature-disabled"));
            return true;
        }

        String permission = permission(type);
        if (!player.hasPermission(permission)) {
            player.sendMessage(plugin.message("no-permission"));
            return true;
        }

        if (poses.isInPose(player, type)) {
            poses.stop(player);
            return true;
        }

        poses.start(player, type);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (type == PoseType.SIT && args.length == 1 && sender.hasPermission("sit.reload")) {
            String value = args[0].toLowerCase();
            return "reload".startsWith(value) ? List.of("reload") : Collections.emptyList();
        }

        return Collections.emptyList();
    }

    private static String permission(PoseType type) {
        return switch (type) {
            case SIT -> "sit.use";
            case LAY -> "sit.lay";
            case SPIN -> "sit.spin";
            case CRAWL -> "sit.crawl";
            case HEAD -> "sit.playersit";
        };
    }
}
