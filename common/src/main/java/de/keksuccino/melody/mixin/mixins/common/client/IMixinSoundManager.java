package de.keksuccino.melody.mixin.mixins.common.client;

import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundManager.class)
public interface IMixinSoundManager {

    @Accessor("soundEngine") SoundEngine getSoundEngineMelody();

}
