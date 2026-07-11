package me.meadow.nms.v26_2;

import me.meadow.Sit;
import me.meadow.nms.NmsBridge;
import me.meadow.nms.NmsCrawl;
import me.meadow.nms.NmsPose;
import me.meadow.pose.PoseType;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class NmsBridge_26_2 implements NmsBridge {
    private final Sit plugin;

    public NmsBridge_26_2(Sit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public Entity createSeat(Player player, Location location, boolean rotate) {
        if (player == null || location == null || location.getWorld() == null || !player.isValid()) {
            return null;
        }

        ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
        SeatEntity seat = new SeatEntity(location);

        if (!level.addFreshEntity(seat)) {
            seat.discard();
            return null;
        }

        Entity bukkitSeat = seat.getBukkitEntity();

        if (!bukkitSeat.addPassenger(player)) {
            seat.discard();
            return null;
        }

        if (rotate) {
            seat.startRotate();
        }

        return bukkitSeat;
    }

    @Override
    public void removeSeat(Entity seat) {
        if (seat == null) {
            return;
        }

        try {
            ((CraftEntity) seat).getHandle().discard();
        } catch (RuntimeException ignored) {
            seat.remove();
        }
    }

    @Override
    public NmsPose createPose(Player player, Entity seat, PoseType type, boolean snoring) {
        if (player == null || seat == null || !(type == PoseType.LAY || type == PoseType.SPIN)) {
            return null;
        }

        try {
            PoseRenderer renderer = new PoseRenderer(plugin, player, seat, type, snoring);
            renderer.spawn();
            return renderer;
        } catch (Throwable exception) {
            plugin.getLogger().warning("Could not create " + type + " pose for "
                    + player.getName() + ": " + exception.getClass().getSimpleName()
                    + ": " + exception.getMessage());
            return null;
        }
    }

    @Override
    public Entity createHeadSeat(Player rider, Player carrier) {
        if (rider == null || carrier == null || !rider.isValid() || !carrier.isValid()) {
            return null;
        }

        Location location = carrier.getLocation();
        if (location.getWorld() == null) {
            return null;
        }

        ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
        HeadSeatEntity seat = new HeadSeatEntity(location);

        if (!level.addFreshEntity(seat)) {
            seat.discard();
            return null;
        }

        Entity bukkitSeat = seat.getBukkitEntity();

        if (!carrier.addPassenger(bukkitSeat)) {
            seat.discard();
            return null;
        }

        if (!bukkitSeat.addPassenger(rider)) {
            bukkitSeat.leaveVehicle();
            seat.discard();
            return null;
        }

        return bukkitSeat;
    }

    @Override
    public NmsCrawl createCrawl(Player player) {
        if (player == null || !player.isValid()) {
            return null;
        }

        try {
            CrawlRenderer crawl = new CrawlRenderer(plugin, player);
            crawl.start();
            return crawl;
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Could not create crawl for " + player.getName() + ": " + exception.getMessage());
            return null;
        }
    }
}
