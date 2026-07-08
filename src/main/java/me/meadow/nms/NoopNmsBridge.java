package me.meadow.nms;

import me.meadow.pose.PoseType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class NoopNmsBridge implements NmsBridge {
    @Override
    public boolean available() {
        return false;
    }

    @Override
    public Entity createSeat(Player player, Location location, boolean rotate) {
        return null;
    }

    @Override
    public void removeSeat(Entity seat) {
        if (seat != null) {
            seat.remove();
        }
    }

    @Override
    public NmsPose createPose(Player player, Entity seat, PoseType type, boolean snoring) {
        return null;
    }

    @Override
    public NmsCrawl createCrawl(Player player) {
        return null;
    }

    @Override
    public Entity createHeadSeat(Player rider, Player carrier) {
        return null;
    }
}
