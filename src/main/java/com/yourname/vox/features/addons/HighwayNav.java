package com.yourname.vox.features.addons;

import com.yourname.vox.IVoxAddon;
import com.yourname.vox.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HighwayNav implements IVoxAddon {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private int targetX = 0;
    private int targetZ = 0;
    private boolean isBreaking = false;
    private BlockPos breakingPos = null;
    private int breakTicks = 0;
    private static final int BREAK_DELAY = 5;
    private List<BlockPos> path = new ArrayList<>();
    private int pathIndex = 0;
    private int statusTicks = 0;
    private String movementMode = "walking"; // walking, running, elytra_bounce
    private boolean isBouncing = false;
    private float bounceTimer = 0;

    // Highway definitions (Nether coordinates)
    private static final List<Integer> MAJOR_AXES = Arrays.asList(0);
    private static final List<Integer> SQUARE_RINGS = Arrays.asList(1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 11000, 12000, 13000, 14000, 15000, 16000, 17000, 18000, 19000, 20000, 21000, 22000, 23000, 24000, 25000, 26000, 27000, 28000, 29000, 30000, 35000, 40000, 45000, 50000, 125000);
    private static final List<Integer> DIAMOND_RINGS = Arrays.asList(5000, 25000, 50000);
    private static final int GRID_SPACING = 5000;

    @Override
    public String getName() {
        return "HighwayNav";
    }

    @Override
    public void onEnable() {
        targetX = ConfigManager.addonSettings.getOrDefault("HighwayNav_targetX", 0).hashCode();
        targetZ = ConfigManager.addonSettings.getOrDefault("HighwayNav_targetZ", 0).hashCode();
        movementMode = ConfigManager.addonSettings.getOrDefault("HighwayNav_mode", "walking").toString();
        isBreaking = false;
        breakingPos = null;
        breakTicks = 0;
        path.clear();
        pathIndex = 0;
        statusTicks = 0;
        bounceTimer = 0;
        isBouncing = false;
        if (targetX != 0 || targetZ != 0) {
            calculatePath();
        }
    }

    @Override
    public void onTick() {
        if (!ConfigManager.addonToggles.getOrDefault(getName(), false) || mc.player == null || mc.world == null) return;

        ClientPlayerEntity player = mc.player;
        Vec3d pos = player.getPos();

        if (targetX == 0 && targetZ == 0) {
            player.sendMessage(Text.literal("HighwayNav: Set target with /vox highwaynav <x> <z>"), false);
            ConfigManager.addonToggles.put(getName(), false);
            return;
        }

        if (path.isEmpty()) {
            calculatePath();
            if (path.isEmpty()) {
                player.sendMessage(Text.literal("HighwayNav: No valid path found"), false);
                ConfigManager.addonToggles.put(getName(), false);
                return;
            }
        }

        if (pathIndex >= path.size()) {
            player.sendMessage(Text.literal("HighwayNav: Reached destination!"), false);
            ConfigManager.addonToggles.put(getName(), false);
            return;
        }

        BlockPos nextPos = path.get(pathIndex);
        double deltaX = nextPos.getX() + 0.5 - pos.x;
        double deltaZ = nextPos.getZ() + 0.5 - pos.z;

        if (Math.abs(deltaX) < 0.3 && Math.abs(deltaZ) < 0.3) {
            pathIndex++;
            return;
        }

        BlockPos frontPos = player.getBlockPos().offset(player.getHorizontalFacing());
        if (isBreaking) {
            handleBreaking(player, frontPos);
        } else if (mc.world.getBlockState(frontPos).isSolidBlock(mc.world, frontPos)) {
            handleObstacle(player, frontPos);
        } else {
            float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
            switch (movementMode) {
                case "elytra_bounce":
                    if (player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                        handleElytraBounce(player, yaw);
                    } else {
                        movePlayer(player, yaw, "walking"); // Fallback if no Elytra
                    }
                    break;
                case "running":
                    movePlayer(player, yaw, "running");
                    break;
                default:
                    movePlayer(player, yaw, "walking");
            }
        }

        // Status update every 5 seconds (100 ticks)
        statusTicks++;
        if (statusTicks >= 100) {
            double dist = Math.sqrt(Math.pow(targetX - pos.x, 2) + Math.pow(targetZ - pos.z, 2));
            player.sendMessage(Text.literal("HighwayNav: " + (int) dist + " blocks to target"), false);
            statusTicks = 0;
        }
    }

    private void calculatePath() {
        path.clear();
        pathIndex = 0;
        if (mc.player == null) return;

        Vec3d startPos = mc.player.getPos();
        int startX = (int) startPos.x;
        int startZ = (int) startPos.z;

        int nearestStartX = findNearestHighway(startX, true);
        int nearestStartZ = findNearestHighway(startZ, false);
        int nearestTargetX = findNearestHighway(targetX, true);
        int nearestTargetZ = findNearestHighway(targetZ, false);

        double diagDist = Math.sqrt(Math.pow(targetX - startX, 2) + Math.pow(targetZ - startZ, 2));
        double straightDist = Math.abs(targetX - startX) + Math.abs(targetZ - startZ);
        if (diagDist < straightDist * 0.8 && Math.abs(targetX) == Math.abs(targetZ)) {
            addDiagonalPath(startX, startZ, targetX, targetZ);
        } else {
            addPathSegment(startX, startZ, nearestStartX, nearestStartZ);
            addPathSegment(nearestStartX, nearestStartZ, nearestTargetX, nearestTargetZ);
            addPathSegment(nearestTargetX, nearestTargetZ, targetX, targetZ);
        }
    }

    private int findNearestHighway(int coord, boolean isX) {
        List<Integer> highways = new ArrayList<>(MAJOR_AXES);
        highways.addAll(SQUARE_RINGS);
        highways.addAll(DIAMOND_RINGS);
        for (int i = -10; i <= 10; i++) {
            highways.add(i * GRID_SPACING);
        }

        int nearest = highways.get(0);
        int minDist = Math.abs(coord - nearest);
        for (int hwy : highways) {
            int dist = Math.abs(coord - hwy);
            if (dist < minDist) {
                minDist = dist;
                nearest = hwy;
            }
        }
        return nearest;
    }

    private void addPathSegment(int startX, int startZ, int endX, int endZ) {
        int steps = Math.max(Math.abs(endX - startX), Math.abs(endZ - startZ));
        for (int i = 0; i <= steps; i += 2) {
            float t = steps == 0 ? 0 : (float) i / steps;
            int x = Math.round(MathHelper.lerp(t, startX, endX));
            int z = Math.round(MathHelper.lerp(t, startZ, endZ));
            path.add(new BlockPos(x, mc.player.getBlockPos().getY(), z));
        }
    }

    private void addDiagonalPath(int startX, int startZ, int endX, int endZ) {
        int steps = Math.max(Math.abs(endX - startX), Math.abs(endZ - startZ));
        for (int i = 0; i <= steps; i += 2) {
            float t = steps == 0 ? 0 : (float) i / steps;
            int x = Math.round(MathHelper.lerp(t, startX, endX));
            int z = Math.round(MathHelper.lerp(t, startZ, endZ));
            path.add(new BlockPos(x, mc.player.getBlockPos().getY(), z));
        }
    }

    private void handleObstacle(ClientPlayerEntity player, BlockPos obstaclePos) {
        if (player.getMainHandStack().getItem() == Items.DIAMOND_PICKAXE || player.getMainHandStack().getItem() == Items.NETHERITE_PICKAXE) {
            startBreaking(player, obstaclePos);
        } else {
            detourAround(player, obstaclePos);
        }
    }

    private void startBreaking(ClientPlayerEntity player, BlockPos pos) {
        isBreaking = true;
        breakingPos = pos;
        breakTicks = 0;
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
    }

    private void handleBreaking(ClientPlayerEntity player, BlockPos frontPos) {
        if (!breakingPos.equals(frontPos)) {
            isBreaking = false;
            return;
        }
        breakTicks++;
        if (breakTicks >= BREAK_DELAY) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, breakingPos, Direction.UP));
            player.swingHand(Hand.MAIN_HAND);
            isBreaking = false;
            breakingPos = null;
            pathIndex--;
            calculatePath();
        }
    }

    private void detourAround(ClientPlayerEntity player, BlockPos obstaclePos) {
        Direction detourDir = player.getHorizontalFacing().rotateYClockwise();
        Vec3d detourPos = player.getPos().add(detourDir.getVector().getX() * 2, 0, detourDir.getVector().getZ() * 2);
        float detourYaw = (float) Math.toDegrees(Math.atan2(detourPos.z - player.getZ(), detourPos.x - player.getX())) - 90.0f;
        movePlayer(player, detourYaw, movementMode);
        calculatePath();
    }

    private void movePlayer(ClientPlayerEntity player, float targetYaw, String mode) {
        player.setYaw(targetYaw);
        player.setPitch(0.0f);
        float speed = ConfigManager.addonToggles.getOrDefault("Speed", false) ? 0.5f : (mode.equals("running") ? 0.3f : 0.1f);
        player.forwardSpeed = speed;
    }

    private void handleElytraBounce(ClientPlayerEntity player, float targetYaw) {
        player.setYaw(targetYaw);
        float speed = ConfigManager.addonToggles.getOrDefault("Speed", false) ? 0.5f : 0.3f;
        if (!player.isFallFlying()) {
            player.startFallFlying();
        }

        bounceTimer += 0.1f;
        if (player.isOnGround() && bounceTimer > 0.2f) {
            player.jump();
            isBouncing = true;
            bounceTimer = 0;
        } else if (!player.isOnGround() && isBouncing && bounceTimer > 0.1f) {
            player.setPitch(-20.0f);
            Vec3d motion = player.getVelocity();
            player.setVelocity(motion.x + Math.sin(Math.toRadians(targetYaw)) * speed, motion.y + 0.1, motion.z + Math.cos(Math.toRadians(targetYaw)) * speed);
            isBouncing = false;
        }
    }

    @Override
    public void onChat(String msg) {
        if (msg.startsWith("/vox highwaynav")) {
            String[] parts = msg.split(" ");
            if (parts.length == 4 && parts[1].equals("mode")) {
                String mode = parts[2].toLowerCase();
                if (List.of("walking", "running", "elytra_bounce").contains(mode)) {
                    movementMode = mode;
                    ConfigManager.addonSettings.put("HighwayNav_mode", mode);
                    mc.player.sendMessage(Text.literal("HighwayNav: Mode set to " + mode), false);
                } else {
                    mc.player.sendMessage(Text.literal("HighwayNav: Invalid mode. Use walking, running, or elytra_bounce"), false);
                }
            } else if (parts.length == 4) {
                try {
                    targetX = Integer.parseInt(parts[2]);
                    targetZ = Integer.parseInt(parts[3]);
                    ConfigManager.addonSettings.put("HighwayNav_targetX", targetX);
                    ConfigManager.addonSettings.put("HighwayNav_targetZ", targetZ);
                    mc.player.sendMessage(Text.literal("HighwayNav: Target set to " + targetX + ", " + targetZ), false);
                    ConfigManager.addonToggles.put(getName(), true);
                    calculatePath();
                } catch (NumberFormatException e) {
                    mc.player.sendMessage(Text.literal("HighwayNav: Invalid coordinates. Use /vox highwaynav <x> <z>"), false);
                }
            }
        }
    }

    @Override
    public void onRenderWorldLast(float partialTicks) {
        // Placeholder for rendering path if desired
    }

    @Override
    public void toggle() {
        ConfigManager.addonToggles.put(getName(), !ConfigManager.addonToggles.getOrDefault(getName(), false));
    }

    @Override
    public String getDescription() {
        return "Navigates highways to coordinates with walking, running, or Elytra Bounce modes, breaking or avoiding obstacles.";
    }
}