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
            "fastleafdecay",  // same-role mod id
            "fldf",  // same-role mod id
            "rapid_leaf_decay",  // same-role mod id
            "quickleafdecay",  // same-role mod id
            "leaves_us_in_peace",  // same-role mod id
            "leavesbegone",  // same-role mod id
            "minimalism_leaf_decay",  // Minimalism Leaf Decay
            "instant-leaf-decay",     // Instant Leaf Decay
            "fast-leaf-decay",  // same-role mod id
            "mr_fast_leaf_decay"  // same-role mod id
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
