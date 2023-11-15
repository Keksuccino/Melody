package de.keksuccino.melody.resources.audio.openal;

import com.mojang.blaze3d.audio.Library;
import de.keksuccino.melody.mixin.mixins.common.client.IMixinSoundEngine;
import de.keksuccino.melody.mixin.mixins.common.client.IMixinSoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.openal.AL10;
import javax.sound.sampled.AudioFormat;
import java.io.InputStream;
import java.nio.ByteBuffer;

@SuppressWarnings("unused")
public class ALUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * OpenAL gets initialized in {@link Library#init(String, boolean)}, which gets called when (re-)loading the {@link SoundEngine},
     * so this method checks for {@link IMixinSoundEngine#getLoadedMelody()}, to see if the {@link SoundEngine} is loaded.
     */
    public static boolean isOpenAlReady() {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        SoundEngine engine = ((IMixinSoundManager)manager).getSoundEngineMelody();
        return ((IMixinSoundEngine)engine).getLoadedMelody();
    }

    public static int getAudioFormatAsOpenAL(@NotNull AudioFormat audioFormat) throws ALException {
        AudioFormat.Encoding encoding = audioFormat.getEncoding();
        int channels = audioFormat.getChannels();
        int sampleSize = audioFormat.getSampleSizeInBits();
        if (encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED) || encoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
            if (channels == 1) {
                if (sampleSize == 8) {
                    return AL10.AL_FORMAT_MONO8;
                }
                if (sampleSize == 16) {
                    return AL10.AL_FORMAT_MONO16;
                }
            } else if (channels == 2) {
                if (sampleSize == 8) {
                    return AL10.AL_FORMAT_STEREO8;
                }
                if (sampleSize == 16) {
                    return AL10.AL_FORMAT_STEREO16;
                }
            }
        }
        throw new ALException("Failed to convert AudioFormat to OpenAL! Unsupported format: " + audioFormat);
    }

    @NotNull
    public static ByteBuffer readStreamIntoBuffer(@NotNull InputStream audioInputStream) throws Exception {
        byte[] array = audioInputStream.readAllBytes();
        //It's important to allocate the buffer, because otherwise OpenAL can't read it
        ByteBuffer buffer = ByteBuffer.allocateDirect(array.length);
        buffer.put(array);
        buffer.flip();
        return buffer;
    }

}
