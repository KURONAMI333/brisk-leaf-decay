package com.kuronami.briskleafdecay;

import com.kuronami.briskleafdecay.platform.Services;

import java.util.List;

/**
 * Shared state for every loader. The leaf mixin asks {@link #isActive()} on each scheduled leaf tick.
 */
public final class BriskLeafDecayCommon {

    /**
     * Mods that already speed up leaf decay. If one of them is installed we stand down, so the two
     * never fight over the same leaves (the user picked that mod on purpose).
     */
    static final List<String> EQUIVALENT_MODS = List.of(
            "fastleafdecay",          // Fast Leaf Decay (Olafski, CurseForge)
            "fldf",                   // Fast Leaf Decay (Modrinth "fld")
            "rapid_leaf_decay",       // Rapid Leaf Decay
            "quickleafdecay",         // Quick Leaf Decay
            "leaves_us_in_peace",     // Leaves Us In Peace
            "leavesbegone",           // Leaves Be Gone
            "minimalism_leaf_decay",  // Minimalism Leaf Decay
            "instant-leaf-decay",     // Instant Leaf Decay
            "fast-leaf-decay",        // Fast Leaf Decay (Fabric port) / Mildly Fast Leaf Decay
            "mr_fast_leaf_decay"      // Fast Leaf Decay (datapack-as-mod build)
    );

    /** Shortest wait, in ticks, between a leaf losing its tree and falling. */
    public static final int MIN_DELAY_TICKS = 2;
    /** Longest wait, in ticks. The spread keeps a canopy from vanishing in a single frame. */
    public static final int MAX_DELAY_TICKS = 8;

    private static volatile Boolean active;

    private BriskLeafDecayCommon() {
    }

    public static void init() {
        resolve();
    }

    public static boolean isActive() {
        Boolean value = active;
        return value != null ? value : resolve();
    }

    private static synchronized boolean resolve() {
        if (active == null) {
            String found = EQUIVALENT_MODS.stream().filter(Services.PLATFORM::isModLoaded).findFirst().orElse(null);
            if (found != null) {
                Constants.LOG.info("'{}' is installed and already handles leaf decay; {} stays inactive.", found, Constants.MOD_NAME);
            }
            active = found == null;
        }
        return active;
    }
}
