package me.meadow.pose;

import me.meadow.Sit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.entity.EntityDismountEvent;

public final class PoseListener implements Listener {
    private final Sit plugin;
    private final PoseManager poses;

    public PoseListener(Sit plugin, PoseManager poses) {
        this.plugin = plugin;
        this.poses = poses;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }

        Player player = event.getPlayer();

        if (player.isOnGround() && !player.isFlying() && !player.isGliding()) {
            poses.stopHeadRiders(player);
        }

        poses.stop(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && !player.isInsideVehicle()) {
                poses.stop(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            poses.stopHeadRiders(player);
            poses.stop(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        poses.stopHeadRiders(event.getEntity());
        poses.stop(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        poses.stopHeadRiders(event.getPlayer());
        poses.stop(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent event) {
        poses.stopHeadRiders(event.getPlayer());
        poses.stop(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        poses.stopHeadRiders(event.getPlayer());
        poses.stop(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        poses.stopByBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerRightClick(PlayerInteractEntityEvent event) {
        if (!plugin.featureEnabled(PoseType.HEAD)) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!(event.getRightClicked() instanceof Player target)) {
            return;
        }

        Player player = event.getPlayer();

        if (!player.hasPermission("sit.playersit")) {
            return;
        }

        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            return;
        }

        if (!plugin.playerSitSettings().enabled(player) || !plugin.playerSitSettings().enabled(target)) {
            return;
        }

        if (poses.startHeadSit(player, target)) {
            event.setCancelled(true);
        }
    }
}
