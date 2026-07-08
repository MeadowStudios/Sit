package me.meadow.nms.v26_1_2;

import java.util.Set;
import me.meadow.Sit;
import me.meadow.nms.NmsCrawl;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class CrawlRenderer implements NmsCrawl {
    private final Sit plugin;
    private final Player player;
    private final ServerPlayer serverPlayer;
    private final BoxEntity box;
    private final Listener listener;
    private boolean boxVisible;
    private boolean finished;
    private boolean stopQueued;

    public CrawlRenderer(Sit plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.serverPlayer = ((CraftPlayer) player).getHandle();
        this.box = new BoxEntity(player.getLocation());

        this.listener = new Listener() {
            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onToggleSwim(EntityToggleSwimEvent event) {
                if (event.getEntity() == player) {
                    event.setCancelled(true);
                }
            }

            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onMove(PlayerMoveEvent event) {
                if (event.getPlayer() != player) {
                    return;
                }

                Location from = event.getFrom();
                Location to = event.getTo();

                if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                    tick(to);
                }
            }
        };
    }

    @SuppressWarnings("deprecation")
    public void start() {
        player.setSwimming(true);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        Bukkit.getScheduler().runTask(plugin, () -> tick(player.getLocation()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void stop() {
        finished = true;
        HandlerList.unregisterAll(listener);
        player.setSwimming(false);
        destroyBox();
    }

    private void tick(Location location) {
        if (finished) {
            return;
        }

        if (!player.isOnline()) {
            destroyBox();
            return;
        }

        if (player.isFlying() || player.isGliding() || serverPlayer.isInWater()) {
            stopPoseNextTick();
            return;
        }

        Location tickLocation = location.clone();
        Block locationBlock = tickLocation.getBlock();
        int blockSize = (int) ((tickLocation.getY() - tickLocation.getBlockY()) * 100.0D);

        tickLocation.setY(tickLocation.getBlockY() + (blockSize >= 40 ? 2.49D : 1.49D));
        Block aboveBlock = tickLocation.getBlock();

        boolean hasSolidBlockAbove = aboveBlock.getBoundingBox().contains(tickLocation.toVector())
                && !aboveBlock.getCollisionShape().getBoundingBoxes().isEmpty();

        if (hasSolidBlockAbove) {
            destroyBox();
            return;
        }

        Location boxLocation = location.clone();
        int height = locationBlock.getBoundingBox().getHeight() >= 0.4D
                || boxLocation.getY() % 0.015625D == 0.0D
                ? (player.getFallDistance() > 0.7F ? 0 : blockSize)
                : 0;

        boxLocation.setY(boxLocation.getY() + (height >= 40 ? 1.5D : 0.5D));
        box.setRawPeekAmount(height >= 40 ? 100 - height : 0);

        if (!boxVisible) {
            box.setPos(boxLocation.getX(), boxLocation.getY(), boxLocation.getZ());
            serverPlayer.connection.send(new ClientboundAddEntityPacket(
                    box.getId(),
                    box.getUUID(),
                    box.getX(),
                    box.getY(),
                    box.getZ(),
                    box.getXRot(),
                    box.getYRot(),
                    box.getType(),
                    0,
                    box.getDeltaMovement(),
                    box.getYHeadRot()
            ));
            boxVisible = true;
            serverPlayer.connection.send(new ClientboundSetEntityDataPacket(box.getId(), box.getEntityData().getNonDefaultValues()));
            return;
        }

        serverPlayer.connection.send(new ClientboundSetEntityDataPacket(box.getId(), box.getEntityData().getNonDefaultValues()));
        box.setPosRaw(boxLocation.getX(), boxLocation.getY(), boxLocation.getZ());
        serverPlayer.connection.send(new ClientboundTeleportEntityPacket(
                box.getId(),
                net.minecraft.world.entity.PositionMoveRotation.of(box),
                Set.of(),
                false
        ));
    }

    private void stopPoseNextTick() {
        if (finished || stopQueued) {
            return;
        }

        stopQueued = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            stopQueued = false;

            if (!finished && player.isOnline()) {
                plugin.poses().stop(player);
            }
        });
    }

    private void destroyBox() {
        if (!boxVisible) {
            return;
        }

        serverPlayer.connection.send(new ClientboundRemoveEntitiesPacket(box.getId()));
        boxVisible = false;
    }
}
