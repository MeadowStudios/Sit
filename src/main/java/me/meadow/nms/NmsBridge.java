package me.meadow.nms;

import me.meadow.pose.PoseType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface NmsBridge {
    boolean available();

    Entity createSeat(Player player, Location location, boolean rotate);

    Entity createHeadSeat(Player rider, Player carrier);

    void removeSeat(Entity seat);

    NmsPose createPose(Player player, Entity seat, PoseType type, boolean snoring);

    NmsCrawl createCrawl(Player player);
}
