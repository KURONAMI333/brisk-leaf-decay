package com.kuronami.briskleafdecay;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class BriskLeafDecay {

    public BriskLeafDecay(IEventBus eventBus) {
        BriskLeafDecayCommon.init();
    }
}
