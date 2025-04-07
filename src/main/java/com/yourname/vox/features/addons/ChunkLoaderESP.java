package com.yourname.vox.features.addons;

import com.yourname.vox.IVoxAddon;
import com.yourname.vox.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.WorldChunk;

public class ChunkLoaderESP implements IVoxAddon {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    @Override
    public String getName() {
        return "ChunkLoaderESP";
    }

    @Override
    public void onEnable() {}

    @Override
    public void onTick() {}

    @Override
    public void onChat(String msg) {}

    @Override
    public void onRenderWorldLast(float partialTicks) {
        if (mc.world != null && mc.player != null && ConfigManager.addonToggles.getOrDefault(getName(), false)) {
            int radius = 8;
            int chunkX = mc.player.getChunkPos().x;
            int chunkZ = mc.player.getChunkPos().z;

            for (int x = chunkX - radius; x <= chunkX + radius; x++) {
                for (int z = chunkZ - radius; z <= chunkZ + radius; z++) {
                    WorldChunk chunk = mc.world.getChunk(x, z);
                    if (chunk != null) {
                        int entityCount = mc.world.getEntitiesByClass(
                                net.minecraft.entity.Entity.class,
                                new Box(
                                        chunk.getPos().getStartX(), 0, chunk.getPos().getStartZ(),
                                        chunk.getPos().getEndX(), mc.world.getHeight(), chunk.getPos().getEndZ()
                                ),
                                entity -> true
                        ).size();
                        if (entityCount > 10) {
                            // Add rendering logic (e.g., draw chunk outline)
                        }
                    }
                }
            }
        }
    }

    @Override
    public void toggle() {
        ConfigManager.addonToggles.put(getName(), !ConfigManager.addonToggles.getOrDefault(getName(), false));
    }

    @Override
    public String getDescription() {
        return "Highlights chunks with high entity counts.";
    }
}