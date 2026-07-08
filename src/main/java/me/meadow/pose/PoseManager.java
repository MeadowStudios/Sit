package me.meadow.pose;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import me.meadow.Sit;
import me.meadow.nms.NmsBridge;
import me.meadow.nms.NmsBridges;
import me.meadow.nms.NmsCrawl;
import me.meadow.nms.NmsPose;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class PoseManager {
    private final Sit plugin;
    private final NmsBridge nms;
    private final Map<UUID, PoseSession> sessions = new HashMap<>();
    private final Map<UUID, Entity> headSeats = new HashMap<>();
    private final Map<UUID, UUID> headRiders = new HashMap<>();
    private static final String HEAD_SEAT_TAG = Sit.PLUGIN_TAG + "_head_seat";

    public PoseManager(Sit plugin) {
        this.plugin = plugin;
        this.nms = NmsBridges.create(plugin);
    }

    public boolean start(Player player, PoseType type) {
        if (player == null || !player.isOnline() || !player.isValid()) {
            return false;
        }

        if (!nms.available()) {
            return false;
        }

        if (player.isSneaking() || player.isDead() || player.isSleeping()) {
            return false;
        }

        if (player.getVehicle() != null || player.isInsideVehicle()) {
            return false;
        }

        if (type != PoseType.CRAWL && !player.isOnGround()) {
            return false;
        }

        if (type == PoseType.CRAWL && (player.isFlying() || player.isGliding())) {
            return false;
        }

        player.setSprinting(false);
        stop(player);

        if (type == PoseType.CRAWL) {
            NmsCrawl crawl = nms.createCrawl(player);
            if (crawl == null) {
                return false;
            }

            sessions.put(player.getUniqueId(), new PoseSession(
                    player.getUniqueId(),
                    type,
                    null,
                    null,
                    crawl,
                    blockBelow(player)
            ));

            hint(player, type);
            return true;
        }


        Location seatLocation = seatLocation(player);
        Entity seat = nms.createSeat(player, seatLocation, type == PoseType.SIT || type == PoseType.SPIN);
        if (seat == null) {
            return false;
        }

        NmsPose pose = null;
        if (type == PoseType.LAY || type == PoseType.SPIN) {
            pose = nms.createPose(player, seat, type, plugin.laySnoringSound());
            if (pose == null) {
                player.leaveVehicle();
                nms.removeSeat(seat);
                return false;
            }
        }

        sessions.put(player.getUniqueId(), new PoseSession(
                player.getUniqueId(),
                type,
                seat,
                pose,
                null,
                blockBelow(player)
        ));

        hint(player, type);
        return true;
    }

    public boolean startHeadSit(Player player, Player target) {
        if (!plugin.featureEnabled(PoseType.HEAD)) {
            return false;
        }

        if (player == null || target == null || player.equals(target)) {
            return false;
        }

        if (!player.isOnline() || !target.isOnline() || !player.isValid() || !target.isValid()) {
            return false;
        }

        if (player.isDead() || target.isDead() || player.isSneaking()) {
            return false;
        }

        if (player.getVehicle() != null || player.isInsideVehicle()) {
            return false;
        }

        if (!plugin.playerSitSettings().enabled(player) || !plugin.playerSitSettings().enabled(target)) {
            return false;
        }

        if (!player.getPassengers().isEmpty() || directHeadRider(player) != null) {
            return false;
        }

        PoseSession targetSession = sessions.get(target.getUniqueId());
        if (blocksHeadSitCarrierSession(targetSession)) {
            return false;
        }

        if (hasNonHeadSeatPassenger(target)) {
            return false;
        }

        Player carrier = highestHeadCarrier(target);
        if (carrier == null || carrier.equals(player)) {
            return false;
        }

        if (!plugin.playerSitSettings().enabled(carrier)) {
            return false;
        }

        PoseSession carrierSession = sessions.get(carrier.getUniqueId());
        if (blocksHeadSitCarrierSession(carrierSession)) {
            return false;
        }

        if (!carrier.getPassengers().isEmpty()) {
            return false;
        }

        stop(player);

        Entity headSeat = nms.createHeadSeat(player, carrier);
        if (headSeat == null) {
            return false;
        }

        headSeats.put(player.getUniqueId(), headSeat);
        headRiders.put(carrier.getUniqueId(), player.getUniqueId());

        carrier.sendActionBar(Component.text("ꜱᴏᴍᴇᴏɴᴇ ꜱɪᴛꜱ ᴏɴ ʏᴏᴜ"));

        sessions.put(player.getUniqueId(), new PoseSession(
                player.getUniqueId(),
                PoseType.HEAD,
                carrier,
                null,
                null,
                null
        ));

        hint(player, PoseType.HEAD);
        return true;
    }

    public void stop(Player player) {
        if (player == null) {
            return;
        }

        PoseSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }

        if (session.nmsCrawl() != null) {
            session.nmsCrawl().stop();
        }

        if (session.nmsPose() != null) {
            session.nmsPose().remove();
        }

        if (session.type() == PoseType.HEAD) {
            stopHeadRiders(player);

            Entity carrier = session.seatEntity();
            if (carrier != null) {
                headRiders.remove(carrier.getUniqueId(), player.getUniqueId());
            } else {
                headRiders.values().remove(player.getUniqueId());
            }

            Entity headSeat = headSeats.remove(player.getUniqueId());

            player.leaveVehicle();

            if (headSeat != null) {
                headSeat.leaveVehicle();
                nms.removeSeat(headSeat);
            }

            return;
        }

        Entity seat = session.seatEntity();
        if (seat != null) {
            player.leaveVehicle();
            nms.removeSeat(seat);
        }
    }

    public void stopByBlock(Block block) {
        if (block == null) {
            return;
        }

        for (PoseSession session : Map.copyOf(sessions).values()) {
            Location location = session.blockLocation();
            if (location == null || location.getWorld() == null) {
                continue;
            }

            if (location.getWorld().equals(block.getWorld())
                    && location.getBlockX() == block.getX()
                    && location.getBlockY() == block.getY()
                    && location.getBlockZ() == block.getZ()) {
                Player player = plugin.getServer().getPlayer(session.playerId());
                if (player != null) {
                    stop(player);
                }
            }
        }
    }

    public boolean hasPose(Player player) {
        return player != null && sessions.containsKey(player.getUniqueId());
    }

    public boolean isInPose(Player player, PoseType type) {
        PoseSession session = player == null ? null : sessions.get(player.getUniqueId());
        return session != null && session.type() == type;
    }

    public PoseSession session(Player player) {
        return player == null ? null : sessions.get(player.getUniqueId());
    }

    public void shutdown() {
        for (UUID uuid : Map.copyOf(sessions).keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                stop(player);
            }
        }

        for (PoseSession session : Map.copyOf(sessions).values()) {
            Entity seat = session.seatEntity();
            if (seat != null) {
                nms.removeSeat(seat);
            }
        }

        for (Entity headSeat : Map.copyOf(headSeats).values()) {
            nms.removeSeat(headSeat);
        }

        headSeats.clear();
        headRiders.clear();
        sessions.clear();
    }

    private void hint(Player player, PoseType type) {
        String hint = plugin.getUpHint(type);
        if (hint != null && !hint.isBlank()) {
            player.sendActionBar(Component.text(hint));
        }
    }

    private Location seatLocation(Player player) {
        Location location = player.getLocation().clone();
        location.setY(location.getY() + 0.05D);
        return location;
    }

    private Location blockBelow(Player player) {
        Location location = player.getLocation().clone();
        location.setY(location.getY() - 0.0625D);
        return location.getBlock().getLocation();
    }

    public void stopHeadRiders(Entity carrier) {
        if (carrier == null) {
            return;
        }

        Set<UUID> stopped = new HashSet<>();

        while (true) {
            Player rider = directHeadRider(carrier);
            if (rider == null || !stopped.add(rider.getUniqueId())) {
                return;
            }

            stop(rider);
        }
    }

    private Player highestHeadCarrier(Player base) {
        Player current = base;
        Set<UUID> seen = new HashSet<>();

        while (current != null) {
            if (!current.isOnline() || !current.isValid()) {
                return null;
            }

            if (!seen.add(current.getUniqueId())) {
                return null;
            }

            Player rider = directHeadRider(current);
            if (rider == null) {
                return current;
            }

            current = rider;
        }

        return null;
    }

    private static boolean hasNonHeadSeatPassenger(Entity entity) {
        if (entity == null) {
            return true;
        }

        for (Entity passenger : entity.getPassengers()) {
            if (!isHeadSeat(passenger)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isHeadSeat(Entity entity) {
        return entity != null && entity.getScoreboardTags().contains(HEAD_SEAT_TAG);
    }

    private static boolean blocksHeadSitCarrierSession(PoseSession session) {
        return session != null
                && session.type() != PoseType.HEAD;
    }

    private Player directHeadRider(Entity carrier) {
        if (carrier == null) {
            return null;
        }

        UUID carrierId = carrier.getUniqueId();
        UUID riderId = headRiders.get(carrierId);
        if (riderId == null) {
            return null;
        }

        PoseSession session = sessions.get(riderId);
        if (session == null) {
            cleanupHeadSession(riderId, carrierId);
            return null;
        }

        if (session.type() != PoseType.HEAD
                || session.seatEntity() == null
                || !session.seatEntity().getUniqueId().equals(carrierId)) {
            headRiders.remove(carrierId, riderId);
            return null;
        }

        Player rider = plugin.getServer().getPlayer(riderId);
        if (rider != null && rider.isOnline() && rider.isValid()) {
            return rider;
        }

        cleanupHeadSession(riderId, carrierId);
        return null;
    }

    private void cleanupHeadSession(UUID riderId, UUID carrierId) {
        sessions.remove(riderId);

        if (carrierId != null) {
            headRiders.remove(carrierId, riderId);
        } else {
            headRiders.values().remove(riderId);
        }

        Entity headSeat = headSeats.remove(riderId);
        if (headSeat != null) {
            headSeat.leaveVehicle();
            nms.removeSeat(headSeat);
        }
    }
}
