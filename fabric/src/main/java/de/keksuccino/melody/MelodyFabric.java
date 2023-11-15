package de.keksuccino.melody;

import net.fabricmc.api.ModInitializer;

public class MelodyFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {

        Melody.init();

    }

}
