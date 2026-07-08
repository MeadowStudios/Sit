package me.meadow.pose;

import java.util.UUID;

import me.meadow.nms.NmsCrawl;
import me.meadow.nms.NmsPose;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public final class PoseSession {
    private final UUID playerId;
    private final PoseType type;
    private final Entity seatEntity;
    private final NmsPose nmsPose;
    private final NmsCrawl nmsCrawl;
    private final Location blockLocation;

    public PoseSession(UUID playerId, PoseType type, Entity seatEntity, NmsPose nmsPose, NmsCrawl nmsCrawl, Location blockLocation) {
        this.playerId = playerId;
        this.type = type;
        this.seatEntity = seatEntity;
        this.nmsPose = nmsPose;
        this.nmsCrawl = nmsCrawl;
        this.blockLocation = blockLocation == null ? null : blockLocation.clone();
    }

    public UUID playerId() {
        return playerId;
    }

    public PoseType type() {
        return type;
    }

    public Entity seatEntity() {
        return seatEntity;
    }

    public NmsPose nmsPose() {
        return nmsPose;
    }

    public NmsCrawl nmsCrawl() {
        return nmsCrawl;
    }

    public Location blockLocation() {
        return blockLocation == null ? null : blockLocation.clone();
    }
}
