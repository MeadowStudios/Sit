package me.meadow.nms.v26_1_2;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;

public final class BoxEntity extends Shulker {
    public BoxEntity(Location location) {
        super(EntityType.SHULKER, ((CraftWorld) location.getWorld()).getHandle());
        setPos(location.getX(), location.getY(), location.getZ());
        setInvisible(true);
        setNoGravity(true);
        setInvulnerable(true);
        setNoAi(true);
        setSilent(true);
        setAttachFace(Direction.UP);
    }

    @Override
    protected void handlePortal() {
    }

    @Override
    public boolean isAffectedByFluids() {
        return false;
    }
}
