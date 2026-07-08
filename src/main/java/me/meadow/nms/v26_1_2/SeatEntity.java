package me.meadow.nms.v26_1_2;

import me.meadow.Sit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.NotNull;

public final class SeatEntity extends ArmorStand {
    private boolean rotate;
    private Runnable runnable;

    public SeatEntity(Location location) {
        super(((CraftWorld) location.getWorld()).getHandle(), location.getX(), location.getY(), location.getZ());

        setInvisible(true);
        setNoGravity(true);
        setMarker(true);
        setInvulnerable(true);
        setSmall(true);
        setNoBasePlate(true);
        setSilent(true);
        setRot(location.getYaw(), location.getPitch());
        yRotO = getYRot();
        setYBodyRot(yRotO);

        if (getAttribute(Attributes.MAX_HEALTH) != null) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(1.0D);
        }

        addTag(Sit.PLUGIN_TAG + "_seat");
    }

    public void startRotate() {
        this.rotate = true;
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void tick() {
        if (runnable != null) {
            runnable.run();
        }

        if (!isAlive() || !rotate) {
            return;
        }

        net.minecraft.world.entity.Entity rider = getFirstPassenger();
        if (rider == null) {
            return;
        }

        setYRot(rider.getYRot());
        yRotO = getYRot();
    }

    @Override
    public void move(@NotNull MoverType moverType, @NotNull Vec3 movement) {
    }

    @Override
    public boolean hurtServer(@NotNull ServerLevel level, @NotNull DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void handlePortal() {
    }

    @Override
    public boolean isAffectedByFluids() {
        return false;
    }

    @Override
    public boolean dismountsUnderwater() {
        return false;
    }
}
