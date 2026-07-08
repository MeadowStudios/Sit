package me.meadow.nms.v26_1_2;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.meadow.Sit;
import me.meadow.nms.NmsPose;
import me.meadow.pose.PoseType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Statistic;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class PoseRenderer implements NmsPose {
    private static final EntityDataAccessor<net.minecraft.world.entity.Pose> POSE_ACCESSOR =
            EntityDataSerializers.POSE.createAccessor(6);
    private static final EntityDataAccessor<Byte> DATA_FLAG_ACCESSOR =
            EntityDataSerializers.BYTE.createAccessor(8);
    private static final EntityDataAccessor<java.util.Optional<BlockPos>> SLEEP_BLOCK_POS_ACCESSOR =
            EntityDataSerializers.OPTIONAL_BLOCK_POS.createAccessor(14);
    private static final EntityDataAccessor<Byte> SKIN_ACCESSOR =
            EntityDataSerializers.BYTE.createAccessor(16);
    private static final int VIEWER_SCAN_INTERVAL_TICKS = 10;

    private int viewerScanCooldown;
    private List<ItemStack> equipmentCache;
    private List<ItemStack> hiddenRealEquipmentCache;
    private final Sit plugin;
    private final Player player;
    private final Entity seatEntity;
    private final SeatEntity nmsSeat;
    private final PoseType type;
    private final boolean snoring;
    private final ServerPlayer serverPlayer;
    private final ServerPlayer fakePlayer;
    private final Location poseLocation;
    private final Location bedLocation;
    private final Block bedBlock;
    private final BlockPos bedPos;
    private final Direction direction;
    private final int renderRange;
    private boolean oldInvisible;

    private Set<Player> viewers = new HashSet<>();
    private ClientboundBundlePacket spawnBundle;
    private ClientboundPlayerInfoRemovePacket removeInfoPacket;
    private ClientboundRemoveEntitiesPacket removeEntityPacket;
    private ClientboundTeleportEntityPacket teleportFakePacket;
    private boolean oldSleepingIgnored;
    private boolean oldGlowing;
    private float lastYaw = Float.MIN_VALUE;

    public PoseRenderer(Sit plugin, Player player, Entity seatEntity, PoseType type, boolean snoring) {
        this.plugin = plugin;
        this.player = player;
        this.seatEntity = seatEntity;
        this.nmsSeat = (SeatEntity) ((CraftEntity) seatEntity).getHandle();
        this.type = type;
        this.snoring = snoring;
        this.serverPlayer = ((CraftPlayer) player).getHandle();

        this.renderRange = Math.max(64, player.getWorld().getSimulationDistance() * 16);
        this.poseLocation = seatEntity.getLocation().clone();

        this.bedLocation = poseLocation.clone();
        this.bedLocation.setY(bedLocation.getWorld().getMinHeight());
        this.bedBlock = bedLocation.getBlock();
        this.bedPos = new BlockPos(bedLocation.getBlockX(), bedLocation.getBlockY(), bedLocation.getBlockZ());
        this.direction = directionFromYaw(poseLocation.getYaw());

        this.fakePlayer = createFakePlayer();
        double y = poseLocation.getY();
        if (type == PoseType.LAY) {
            y += 0.1125D * serverPlayer.getScale();
        }
        fakePlayer.absSnapTo(poseLocation.getX(), y, poseLocation.getZ(), 0.0F, 0.0F);
    }

    public void spawn() {
        this.viewers = nearbyPlayers();
        this.oldSleepingIgnored = player.isSleepingIgnored();
        this.oldGlowing = serverPlayer.hasGlowingTag();
        this.oldInvisible = serverPlayer.isInvisible();

        fakePlayer.setGlowingTag(oldGlowing);
        if (oldGlowing) {
            serverPlayer.setGlowingTag(false);
        }

        fakePlayer.getEntityData().set(POSE_ACCESSOR, nmsPose(type));
        fakePlayer.getEntityData().set(SKIN_ACCESSOR, serverPlayer.getEntityData().get(SKIN_ACCESSOR));

        if (type == PoseType.SPIN) {
            fakePlayer.getEntityData().set(DATA_FLAG_ACCESSOR, (byte) 4);
        }

        if (type == PoseType.LAY) {
            fakePlayer.getEntityData().set(SLEEP_BLOCK_POS_ACCESSOR, java.util.Optional.of(bedPos));
            player.setSleepingIgnored(true);
            player.setStatistic(Statistic.TIME_SINCE_REST, 0);
        }

        serverPlayer.setInvisible(true);

        List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
        packets.add(new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(
                        ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                        ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME
                ),
                Collections.singletonList(fakePlayer)
        ));

        packets.add(new ClientboundAddEntityPacket(
                fakePlayer.getId(),
                fakePlayer.getUUID(),
                fakePlayer.getX(),
                fakePlayer.getY(),
                fakePlayer.getZ(),
                fakePlayer.getXRot(),
                fakePlayer.getYRot(),
                fakePlayer.getType(),
                0,
                fakePlayer.getDeltaMovement(),
                fakePlayer.getYHeadRot()
        ));

        if (type == PoseType.LAY) {
            packets.add(new ClientboundBlockUpdatePacket(
                    bedPos,
                    Blocks.WHITE_BED.defaultBlockState()
                            .setValue(BedBlock.FACING, direction.getOpposite())
                            .setValue(BedBlock.PART, BedPart.HEAD)
            ));

            this.teleportFakePacket = new ClientboundTeleportEntityPacket(
                    fakePlayer.getId(),
                    net.minecraft.world.entity.PositionMoveRotation.of(fakePlayer),
                    Set.of(),
                    false
            );

            packets.add(teleportFakePacket);
        }

        packets.add(new ClientboundSetEntityDataPacket(
                fakePlayer.getId(),
                fakePlayer.getEntityData().isDirty()
                        ? fakePlayer.getEntityData().packDirty()
                        : fakePlayer.getEntityData().getNonDefaultValues()
        ));

        packets.add(new ClientboundUpdateAttributesPacket(fakePlayer.getId(), serverPlayer.getAttributes().getSyncableAttributes()));

        if (type == PoseType.SPIN) {
            packets.add(new ClientboundMoveEntityPacket.PosRot(
                    fakePlayer.getId(),
                    (short) 0,
                    (short) 0,
                    (short) 0,
                    (byte) 0,
                    fixedRotation(-90.0F),
                    true
            ));
        }

        this.spawnBundle = new ClientboundBundlePacket(packets);
        this.removeInfoPacket = new ClientboundPlayerInfoRemovePacket(Collections.singletonList(fakePlayer.getUUID()));
        this.removeEntityPacket = new ClientboundRemoveEntitiesPacket(fakePlayer.getId());

        for (Player viewer : viewers) {
            addViewer(viewer);
        }

        equipmentCache = currentEquipmentCopy();
        hiddenRealEquipmentCache = currentEquipmentCopy();

        nmsSeat.setRunnable(this::tick);
    }

    @Override
    public void remove() {
        nmsSeat.setRunnable(null);

        Set<Player> oldViewers = new HashSet<>(viewers);

        for (Player viewer : oldViewers) {
            removeViewer(viewer);
        }
        viewers.clear();

        if (type == PoseType.LAY && !oldSleepingIgnored) {
            player.setSleepingIgnored(false);
        }

        serverPlayer.setInvisible(oldInvisible || serverPlayer.getActiveEffectsMap().containsKey(MobEffects.INVISIBILITY));
        serverPlayer.setGlowingTag(oldGlowing);

        for (Player viewer : oldViewers) {
            sendRealEquipmentVisible(viewer, true);
        }
    }

    private void tick() {
        if (!player.isOnline() || !seatEntity.isValid()) {
            return;
        }

        refreshViewersIfNeeded();

        if (!serverPlayer.isInvisible()) {
            serverPlayer.setInvisible(true);
        }

        updateEquipment();
        sendRealEquipmentHidden();
        updateSkin();

        if (type == PoseType.LAY) {
            updateLayDirection();

            if (snoring && serverPlayer.getPlayerTime() % 90L == 0L) {
                for (Player viewer : viewers) {
                    viewer.playSound(poseLocation, Sound.ENTITY_FOX_SLEEP, SoundCategory.PLAYERS, 1.1F, 0.0F);
                }
            }
        }
    }

    private void refreshViewersIfNeeded() {
        if (viewerScanCooldown-- > 0) {
            return;
        }

        viewerScanCooldown = VIEWER_SCAN_INTERVAL_TICKS;

        Set<Player> current = nearbyPlayers();

        for (Player viewer : current) {
            if (viewers.add(viewer)) {
                addViewer(viewer);
            } else {
                sendRealEquipmentVisible(viewer, false);
            }
        }

        for (Player viewer : new HashSet<>(viewers)) {
            if (!current.contains(viewer)) {
                viewers.remove(viewer);
                removeViewer(viewer);
            }
        }
    }

    private void addViewer(Player viewer) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }

        sendPacket(viewer, spawnBundle);
        sendFakeEquipment(viewer);
        sendRealEquipmentVisible(viewer, false);
        sendDelayedRealEquipmentHide(viewer);

        if (type == PoseType.LAY && teleportFakePacket != null) {
            sendDelayedLayTeleport(viewer);
        }
    }

    private void sendDelayedRealEquipmentHide(Player viewer) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !viewer.isOnline() || !viewers.contains(viewer)) {
                return;
            }

            sendRealEquipmentVisible(viewer, false);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || !viewer.isOnline() || !viewers.contains(viewer)) {
                    return;
                }

                sendRealEquipmentVisible(viewer, false);
            }, 1L);
        }, 1L);
    }

    private void sendDelayedLayTeleport(Player viewer) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !viewer.isOnline() || !viewers.contains(viewer)) {
                return;
            }

            sendPacket(viewer, teleportFakePacket);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || !viewer.isOnline() || !viewers.contains(viewer)) {
                    return;
                }

                sendPacket(viewer, teleportFakePacket);
            }, 1L);
        }, 1L);
    }

    private void updateLayDirection() {
        float yaw = player.getLocation().getYaw();

        if (yaw == lastYaw) {
            return;
        }

        lastYaw = yaw;

        float fixedYaw = yaw;

        if (direction == Direction.WEST) {
            fixedYaw -= 90.0F;
        } else if (direction == Direction.EAST) {
            fixedYaw += 90.0F;
        } else if (direction == Direction.NORTH) {
            fixedYaw -= 180.0F;
        }

        fixedYaw = normalizeYaw(fixedYaw);

        float clientYaw = fixedYaw >= 315.0F
                ? fixedYaw - 360.0F
                : fixedYaw <= 45.0F
                ? fixedYaw
                : fixedYaw >= 180.0F
                ? -45.0F
                : 45.0F;

        ClientboundRotateHeadPacket packet =
                new ClientboundRotateHeadPacket(fakePlayer, fixedRotation(clientYaw));

        for (Player viewer : viewers) {
            sendPacket(viewer, packet);
        }
    }

    private static float normalizeYaw(float yaw) {
        return (yaw < 0.0F ? 360.0F + yaw : yaw) % 360.0F;
    }

    private void updateSkin() {
        fakePlayer.setInvisible(serverPlayer.getActiveEffectsMap().containsKey(MobEffects.INVISIBILITY));

        fakePlayer.getEntityData().set(SKIN_ACCESSOR, serverPlayer.getEntityData().get(SKIN_ACCESSOR));

        if (!fakePlayer.getEntityData().isDirty()) {
            return;
        }

        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(fakePlayer.getId(), fakePlayer.getEntityData().packDirty());
        for (Player viewer : viewers) {
            sendPacket(viewer, packet);
        }
    }

    private void updateEquipment() {
        if (sameEquipment()) {
            return;
        }

        equipmentCache = currentEquipmentCopy();

        ClientboundSetEquipmentPacket fakeEquipmentPacket =
                new ClientboundSetEquipmentPacket(fakePlayer.getId(), equipmentList(true));

        for (Player viewer : viewers) {
            sendPacket(viewer, fakeEquipmentPacket);
        }
    }

    private boolean sameEquipment() {
        EquipmentSlot[] slots = EquipmentSlot.values();

        if (equipmentCache == null || equipmentCache.size() != slots.length) {
            return false;
        }

        for (int index = 0; index < slots.length; index++) {
            if (!sameItem(equipmentCache.get(index), serverPlayer.getItemBySlot(slots[index]))) {
                return false;
            }
        }

        return true;
    }

    private List<ItemStack> currentEquipmentCopy() {
        EquipmentSlot[] slots = EquipmentSlot.values();
        List<ItemStack> copy = new ArrayList<>(slots.length);

        for (EquipmentSlot slot : slots) {
            copy.add(serverPlayer.getItemBySlot(slot).copy());
        }

        return copy;
    }

    private static boolean sameItem(ItemStack first, ItemStack second) {
        if (first == null || second == null) {
            return first == second;
        }

        return ItemStack.matches(first, second);
    }

    private void sendFakeEquipment(Player viewer) {
        sendPacket(viewer, new ClientboundSetEquipmentPacket(fakePlayer.getId(), equipmentList(true)));

        if (viewer.equals(player)) {
            sendPacket(viewer, new ClientboundSetEquipmentPacket(serverPlayer.getId(), equipmentList(false)));
        }
    }

    private List<Pair<EquipmentSlot, ItemStack>> equipmentList(boolean visible) {
        List<Pair<EquipmentSlot, ItemStack>> equipment = new ArrayList<>();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            equipment.add(Pair.of(slot, visible ? serverPlayer.getItemBySlot(slot) : ItemStack.EMPTY));
        }

        return equipment;
    }

    private void sendRealEquipmentVisible(Player viewer, boolean visible) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }

        List<Pair<EquipmentSlot, ItemStack>> equipment = new ArrayList<>();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            equipment.add(Pair.of(slot, visible ? serverPlayer.getItemBySlot(slot) : ItemStack.EMPTY));
        }

        sendPacket(viewer, new ClientboundSetEquipmentPacket(serverPlayer.getId(), equipment));
    }

    private void removeViewer(Player viewer) {
        sendPacket(viewer, removeInfoPacket);
        sendPacket(viewer, removeEntityPacket);

        if (type == PoseType.LAY) {
            viewer.sendBlockChange(bedLocation, bedBlock.getBlockData());
        }
    }

    private Set<Player> nearbyPlayers() {
        Set<Player> result = new HashSet<>();
        double rangeSq = (double) renderRange * (double) renderRange;

        for (Player worldPlayer : player.getWorld().getPlayers()) {
            if (worldPlayer.equals(player)) {
                result.add(worldPlayer);
                continue;
            }

            if (worldPlayer.getLocation().distanceSquared(poseLocation) > rangeSq) {
                continue;
            }

            if (worldPlayer.canSee(player)) {
                result.add(worldPlayer);
            }
        }

        return result;
    }

    private ServerPlayer createFakePlayer() {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel level = ((CraftWorld) player.getWorld()).getHandle();

        GameProfile profile = createProfileWithTextures(
                serverPlayer.getGameProfile(),
                java.util.UUID.randomUUID(),
                player.getName()
        );

        ClientInformation clientInformation = serverPlayer.clientInformation();
        ServerPlayer npc = new ServerPlayer(server, level, profile, clientInformation);
        npc.connection = serverPlayer.connection;
        return npc;
    }

    private static GameProfile createProfileWithTextures(GameProfile source, java.util.UUID uuid, String name) {
        Object sourceProperties = profileProperties(source);

        if (sourceProperties != null) {
            GameProfile profile = constructProfile(uuid, name, sourceProperties);
            if (profile != null) {
                return profile;
            }
        }

        GameProfile profile = new GameProfile(uuid, name);
        copyProfileProperties(source, profile);
        return profile;
    }

    private static GameProfile constructProfile(java.util.UUID uuid, String name, Object properties) {
        for (java.lang.reflect.Constructor<?> constructor : GameProfile.class.getConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();

            if (types.length != 3) {
                continue;
            }

            if (types[0] != java.util.UUID.class || types[1] != String.class) {
                continue;
            }

            if (!types[2].isAssignableFrom(properties.getClass())) {
                continue;
            }

            try {
                constructor.setAccessible(true);
                return (GameProfile) constructor.newInstance(uuid, name, properties);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }

        return null;
    }

    private static void copyProfileProperties(GameProfile source, GameProfile target) {
        Object sourceProperties = profileProperties(source);
        Object targetProperties = profileProperties(target);

        if (sourceProperties == null || targetProperties == null) {
            return;
        }

        for (java.lang.reflect.Method method : targetProperties.getClass().getMethods()) {
            if (!method.getName().equals("putAll") || method.getParameterCount() != 1) {
                continue;
            }

            if (!method.getParameterTypes()[0].isAssignableFrom(sourceProperties.getClass())) {
                continue;
            }

            try {
                method.setAccessible(true);
                method.invoke(targetProperties, sourceProperties);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }

            return;
        }
    }

    private static Object profileProperties(GameProfile profile) {
        if (profile == null) {
            return null;
        }

        for (String methodName : new String[]{"getProperties", "properties"}) {
            try {
                java.lang.reflect.Method method = profile.getClass().getMethod(methodName);
                method.setAccessible(true);
                return method.invoke(profile);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }

        for (String fieldName : new String[]{"properties", "propertyMap"}) {
            Class<?> current = profile.getClass();

            while (current != null) {
                try {
                    java.lang.reflect.Field field = current.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field.get(profile);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    current = current.getSuperclass();
                }
            }
        }

        return null;
    }

    private static net.minecraft.world.entity.Pose nmsPose(PoseType type) {
        return switch (type) {
            case LAY -> net.minecraft.world.entity.Pose.SLEEPING;
            case SPIN -> net.minecraft.world.entity.Pose.SPIN_ATTACK;
            default -> net.minecraft.world.entity.Pose.STANDING;
        };
    }

    private static Direction directionFromYaw(float yaw) {
        return (yaw >= 135.0F || yaw < -135.0F)
                ? Direction.NORTH
                : (yaw >= -135.0F && yaw < -45.0F)
                ? Direction.EAST
                : (yaw >= -45.0F && yaw < 45.0F)
                ? Direction.SOUTH
                : Direction.WEST;
    }

    private static byte fixedRotation(float rotation) {
        return (byte) (rotation * 256.0F / 360.0F);
    }

    private void sendPacket(Player target, Packet<? super ClientGamePacketListener> packet) {
        ((CraftPlayer) target).getHandle().connection.send(packet);
    }

    private void sendRealEquipmentHidden() {
        if (sameHiddenRealEquipment()) {
            return;
        }

        hiddenRealEquipmentCache = currentEquipmentCopy();

        ClientboundSetEquipmentPacket hiddenRealEquipmentPacket =
                new ClientboundSetEquipmentPacket(serverPlayer.getId(), equipmentList(false));

        for (Player viewer : viewers) {
            sendPacket(viewer, hiddenRealEquipmentPacket);
        }
    }

    private boolean sameHiddenRealEquipment() {
        EquipmentSlot[] slots = EquipmentSlot.values();

        if (hiddenRealEquipmentCache == null || hiddenRealEquipmentCache.size() != slots.length) {
            return false;
        }

        for (int index = 0; index < slots.length; index++) {
            if (!sameItem(hiddenRealEquipmentCache.get(index), serverPlayer.getItemBySlot(slots[index]))) {
                return false;
            }
        }

        return true;
    }
}
