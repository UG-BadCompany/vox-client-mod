package com.yourname.vox;

import com.yourname.vox.features.addons.*;
import java.util.ArrayList;
import java.util.List;

public class AddonLoader {
    private static final List<IVoxAddon> addons = new ArrayList<>();

    static {
        addons.add(new AntiAFK());
        addons.add(new AntiAnticheat());
        addons.add(new AntiTrap());
        addons.add(new AutoBase());
        addons.add(new AutoNav());
        addons.add(new AutoRespond());
        addons.add(new AutoSurvival());
        addons.add(new AutoTotem());
        addons.add(new BowAimbot());
        addons.add(new ChunkLoaderESP());
        addons.add(new DupeHelper());
        addons.add(new FakeDisconnect());
        addons.add(new Godmode());
        addons.add(new GriefingBot());
        addons.add(new HighwayNav());
        addons.add(new InventorySpoofer());
        addons.add(new KillAura());
        addons.add(new LavaWalker());
        addons.add(new PhaseClip());
        addons.add(new PortalBreaker());
        addons.add(new ServerScan());
        addons.add(new Speed());
        addons.add(new StashFinder());
        addons.add(new StealthSuite());
        addons.add(new Teleport());
    }

    public static List<IVoxAddon> getAddons() {
        return addons;
    }

    public static void loadAddons() {
        System.out.println("Addons loaded: " + addons.size());
    }
}