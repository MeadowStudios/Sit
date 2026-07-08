package me.meadow.nms.v26_1_2;

import me.meadow.Sit;
import net.minecraft.world.entity.AreaEffectCloud;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;

public final class HeadSeatEntity extends AreaEffectCloud {
    public HeadSeatEntity(Location location) {
        super(((CraftWorld) location.getWorld()).getHandle(), location.getX(), location.getY(), location.getZ());

        setRadius(0.0F);
        setDuration(Integer.MAX_VALUE);
        setNoGravity(true);
        setInvulnerable(true);
        setInvisible(true);
        setSilent(true);
        addTag(Sit.PLUGIN_TAG + "_head_seat");
    }

    @Override
    public void tick() {
    }

    @Override
    protected void handlePortal() {
    }

    @Override
    public boolean dismountsUnderwater() {
        return false;
    }
}
