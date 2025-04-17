package com.yourname.vox.features.addons;

import com.yourname.vox.IVoxAddon;
import com.yourname.vox.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.registry.tag.FluidTags;
import java.util.Arrays;

public class HighwayNav implements IVoxAddon {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public static boolean enabled = false;
    public static String axis = "x+"; // Default: X+ (Nether highway)
    public static float speed = 0.1f; // Movement speed (0.05–1.0)
    public static boolean sprint = true; // Auto-sprint
    public static boolean autoJump = true; // Auto-jump over gaps
    public static float pathWidth = 1.0f; // Path width to stay centered (0.5–2.0)
    private boolean navigating = false;
    private BlockPos lastPos = null;
    private int stuckTicks = 0;

    @Override
    public String getName() {
        return "HighwayNav";
    }

    @Override
    public void onEnable() {
        enabled = true;
        navigating = true;
        ConfigManager.addonToggles.put(getName(), enabled);
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("HighwayNav enabled"), false);
            System.out.println("[Vox] HighwayNav enabled, navigating=" + navigating);
        }
        loadConfig();
        ConfigManager.saveConfig();
    }

    @Override
    public void onTick() {
        if (!enabled || mc.player == null || !navigating) {
            System.out.println("[Vox] HighwayNav tick skipped: enabled=" + enabled + ", player=" + (mc.player != null) + ", navigating=" + navigating);
            stopNavigation();
            return;
        }

        System.out.println("[Vox] HighwayNav tick running");
        if (!isSafeToNavigate()) {
            stopNavigation();
            return;
        }

        navigateHighway();
        checkStuck();
    }

    @Override
    public void onChat(String msg) {}

    @Override
    public void onRenderWorldLast(float partialTicks) {}

    @Override
    public void toggle() {
        enabled = !enabled;
        navigating = enabled;
        ConfigManager.addonToggles.put(getName(), enabled);
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("HighwayNav " + (enabled ? "enabled" : "disabled")), false);
            System.out.println("[Vox] HighwayNav toggled, enabled=" + enabled + ", navigating=" + navigating);
        }
        if (!enabled) {
            stopNavigation();
        }
        ConfigManager.saveConfig();
    }

    @Override
    public String getDescription() {
        return "Advanced navigation for 2b2t highways. Axes: x+, x-, z+, z-, x+z+, etc. Configurable sprint, jump, speed, width.";
    }

    public void setAxis(String newAxis) {
        if (Arrays.asList("x+", "x-", "z+", "z-", "x+z+", "x-z+", "x+z-", "x-z-").contains(newAxis.toLowerCase())) {
            axis = newAxis.toLowerCase();
            ConfigManager.addonSettings.put(getName() + "_axis", axis);
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("HighwayNav axis set to " + axis), false);
            }
            ConfigManager.saveConfig();
        } else if (mc.player != null) {
            mc.player.sendMessage(Text.literal("Invalid axis. Use: x+, x-, z+, z-, x+z+, x-z+, x+z-, x-z-"), false);
        }
    }

    public void setSpeed(float newSpeed) {
        if (newSpeed >= 0.05f && newSpeed <= 1.0f) {
            speed = newSpeed;
            ConfigManager.addonSettings.put(getName() + "_speed", speed);
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("HighwayNav speed set to " + speed), false);
            }
            ConfigManager.saveConfig();
        } else if (mc.player != null) {
            mc.player.sendMessage(Text.literal("Speed must be between 0.05 and 1.0"), false);
        }
    }

    public void setSprint(boolean newSprint) {
        sprint = newSprint;
        ConfigManager.addonSettings.put(getName() + "_sprint", sprint);
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("HighwayNav sprint " + (sprint ? "enabled" : "disabled")), false);
        }
        ConfigManager.saveConfig();
    }

    public void setAutoJump(boolean newAutoJump) {
        autoJump = newAutoJump;
        ConfigManager.addonSettings.put(getName() + "_autoJump", autoJump);
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("HighwayNav auto-jump " + (autoJump ? "enabled" : "disabled")), false);
        }
        ConfigManager.saveConfig();
    }

    public void setPathWidth(float newWidth) {
        if (newWidth >= 0.5f && newWidth <= 2.0f) {
            pathWidth = newWidth;
            ConfigManager.addonSettings.put(getName() + "_pathWidth", pathWidth);
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("HighwayNav path width set to " + pathWidth), false);
            }
            ConfigManager.saveConfig();
        } else if (mc.player != null) {
            mc.player.sendMessage(Text.literal("Path width must be between 0.5 and 2.0"), false);
        }
    }

    private boolean isSafeToNavigate() {
        if (mc.player == null || mc.world == null) {
            System.out.println("[Vox] HighwayNav unsafe: player or world null");
            return false;
        }

        // Check Y-level (Nether highway: Y=118–122)
        if (mc.player.getY() < 118.0 || mc.player.getY() > 122.0) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("Please move to Y=118–122 for highway navigation"), false);
            }
            System.out.println("[Vox] HighwayNav unsafe: Y=" + mc.player.getY() + ", expected 118–122");
            return false;
        }

        // Check health
        if (mc.player.getHealth() < 8.0f) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("Low health detected, pausing navigation"), false);
            }
            System.out.println("[Vox] HighwayNav unsafe: health=" + mc.player.getHealth());
            return false;
        }

        // Check footing (avoid falling into lava)
        BlockPos below = mc.player.getBlockPos().down();
        if (mc.world.getBlockState(below).isAir() || mc.world.getBlockState(below).getFluidState().isIn(FluidTags.LAVA)) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("Unsafe footing detected, pausing navigation"), false);
            }
            System.out.println("[Vox] HighwayNav unsafe: footing air or lava at " + below);
            return false;
        }

        System.out.println("[Vox] HighwayNav safe to navigate");
        return true;
    }

    private void navigateHighway() {
        if (mc.player == null || mc.world == null) {
            System.out.println("[Vox] HighwayNav navigateHighway skipped: player or world null");
            return;
        }

        System.out.println("[Vox] HighwayNav navigating, axis=" + axis + ", speed=" + speed + ", sprint=" + sprint);

        // Set player yaw to match axis
        float yaw = switch (axis) {
            case "x+" -> 90.0f;
            case "x-" -> -90.0f;
            case "z+" -> 0.0f;
            case "z-" -> 180.0f;
            case "x+z+" -> 45.0f;
            case "x-z+" -> -45.0f;
            case "x+z-" -> 135.0f;
            case "x-z-" -> -135.0f;
            default -> mc.player.getYaw();
        };
        mc.player.setYaw(yaw);
        System.out.println("[Vox] HighwayNav set yaw=" + yaw);

        // Calculate motion based on axis
        Vec3d motion = new Vec3d(0, 0, 0);
        double adjustedSpeed = sprint ? speed * 1.3 : speed;
        switch (axis) {
            case "x+":
                motion = new Vec3d(adjustedSpeed, 0, 0);
                break;
            case "x-":
                motion = new Vec3d(-adjustedSpeed, 0, 0);
                break;
            case "z+":
                motion = new Vec3d(0, 0, adjustedSpeed);
                break;
            case "z-":
                motion = new Vec3d(0, 0, -adjustedSpeed);
                break;
            case "x+z+":
                motion = new Vec3d(adjustedSpeed / Math.sqrt(2), 0, adjustedSpeed / Math.sqrt(2));
                break;
            case "x-z+":
                motion = new Vec3d(-adjustedSpeed / Math.sqrt(2), 0, adjustedSpeed / Math.sqrt(2));
                break;
            case "x+z-":
                motion = new Vec3d(adjustedSpeed / Math.sqrt(2), 0, -adjustedSpeed / Math.sqrt(2));
                break;
            case "x-z-":
                motion = new Vec3d(-adjustedSpeed / Math.sqrt(2), 0, -adjustedSpeed / Math.sqrt(2));
                break;
        }

        // Center on path
        double centerX = Math.round(mc.player.getX() / pathWidth) * pathWidth;
        double centerZ = Math.round(mc.player.getZ() / pathWidth) * pathWidth;
        double deltaX = centerX - mc.player.getX();
        double deltaZ = centerZ - mc.player.getZ();
        motion = motion.add(deltaX * 0.05, 0, deltaZ * 0.05); // Gentle correction
        System.out.println("[Vox] HighwayNav motion: x=" + motion.x + ", z=" + motion.z);

        // Check for obstacles
        BlockPos front = mc.player.getBlockPos().offset(mc.player.getHorizontalFacing(), 1);
        boolean shouldJump = !mc.world.getBlockState(front).isAir() && autoJump;
        holdJumpKey(shouldJump);
        System.out.println("[Vox] HighwayNav " + (shouldJump ? "jumping over obstacle at " + front : "not jumping"));

        // Apply motion with fallback
        mc.player.setVelocity(motion.x, mc.player.getVelocity().y, motion.z);
        holdForwardKey(true);
        if (sprint) {
            mc.player.setSprinting(true);
        }
        System.out.println("[Vox] HighwayNav applied velocity, sprint=" + mc.player.isSprinting());
    }

    private void stopNavigation() {
        navigating = false;
        holdForwardKey(false);
        holdJumpKey(false);
        if (mc.player != null) {
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
            mc.player.setSprinting(false);
            System.out.println("[Vox] HighwayNav stopped navigation");
        }
        stuckTicks = 0;
        lastPos = null;
    }

    private void checkStuck() {
        if (mc.player == null) {
            System.out.println("[Vox] HighwayNav checkStuck skipped: player null");
            return;
        }

        BlockPos currentPos = mc.player.getBlockPos();
        if (lastPos != null && lastPos.equals(currentPos)) {
            stuckTicks++;
            if (stuckTicks > 100) { // ~5 seconds
                if (mc.player != null) {
                    mc.player.sendMessage(Text.literal("Stuck detected, pausing navigation"), false);
                }
                System.out.println("[Vox] HighwayNav stuck detected, ticks=" + stuckTicks);
                stopNavigation();
            }
        } else {
            stuckTicks = 0;
        }
        lastPos = currentPos;
        System.out.println("[Vox] HighwayNav checkStuck: stuckTicks=" + stuckTicks);
    }

    private void holdForwardKey(boolean hold) {
        KeyBinding forwardKey = mc.options.forwardKey;
        forwardKey.setPressed(hold);
        System.out.println("[Vox] HighwayNav forward key: " + (hold ? "pressed" : "released"));
    }

    private void holdJumpKey(boolean hold) {
        KeyBinding jumpKey = mc.options.jumpKey;
        jumpKey.setPressed(hold);
        System.out.println("[Vox] HighwayNav jump key: " + (hold ? "pressed" : "released"));
    }

    private void loadConfig() {
        Object savedAxis = ConfigManager.addonSettings.get(getName() + "_axis");
        if (savedAxis instanceof String && Arrays.asList("x+", "x-", "z+", "z-", "x+z+", "x-z+", "x+z-", "x-z-").contains(savedAxis)) {
            axis = (String) savedAxis;
        }
        Object savedSpeed = ConfigManager.addonSettings.get(getName() + "_speed");
        if (savedSpeed instanceof Float && (Float) savedSpeed >= 0.05f && (Float) savedSpeed <= 1.0f) {
            speed = (Float) savedSpeed;
        }
        Object savedSprint = ConfigManager.addonSettings.get(getName() + "_sprint");
        if (savedSprint instanceof Boolean) {
            sprint = (Boolean) savedSprint;
        }
        Object savedAutoJump = ConfigManager.addonSettings.get(getName() + "_autoJump");
        if (savedAutoJump instanceof Boolean) {
            autoJump = (Boolean) savedAutoJump;
        }
        Object savedPathWidth = ConfigManager.addonSettings.get(getName() + "_pathWidth");
        if (savedPathWidth instanceof Float && (Float) savedPathWidth >= 0.5f && (Float) savedPathWidth <= 2.0f) {
            pathWidth = (Float) savedPathWidth;
        }
        System.out.println("[Vox] HighwayNav loaded config: axis=" + axis + ", speed=" + speed + ", sprint=" + sprint + ", autoJump=" + autoJump + ", pathWidth=" + pathWidth);
    }
}