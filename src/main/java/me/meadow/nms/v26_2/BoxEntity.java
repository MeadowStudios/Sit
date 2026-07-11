package me.meadow.nms.v26_2;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;

public final class BoxEntity extends Shulker {
    public BoxEntity(Location location) {
        super(shulkerType(), ((CraftWorld) location.getWorld()).getHandle());

        setPos(location.getX(), location.getY(), location.getZ());
        setInvisible(true);
        setNoGravity(true);
        setInvulnerable(true);
        setNoAi(true);
        setSilent(true);
        setAttachFace(Direction.UP);
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends Shulker> shulkerType() {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            String key = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(type));

            if (key.equals("minecraft:shulker") || key.endsWith(":shulker")) {
                return (EntityType<? extends Shulker>) type;
            }
        }

        throw new IllegalStateException("Could not find shulker entity type.");
    }

    @Override
    protected void handlePortal() {
    }

    @Override
    public boolean isAffectedByFluids() {
        return false;
    }
}
