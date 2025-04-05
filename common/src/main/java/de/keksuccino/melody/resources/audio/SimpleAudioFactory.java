package de.keksuccino.melody.resources.audio;

import com.mojang.blaze3d.audio.OggAudioStream;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.melody.resources.audio.openal.ALAudioBuffer;
import de.keksuccino.melody.resources.audio.openal.ALAudioClip;
import de.keksuccino.melody.resources.audio.openal.ALUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class SimpleAudioFactory {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ConsumingSupplier<String, Boolean> BASIC_URL_TEXT_VALIDATOR = consumes -> {
        if ((consumes != null) && !consumes.replace(" ", "").isEmpty()) {
            if ((consumes.startsWith("http://") || consumes.startsWith("https://")) && consumes.contains(".")) return true;
        }
        return false;
    };

    @NotNull
    public static CompletableFuture<ALAudioClip> ogg(@NotNull String audioSource, @NotNull SourceType sourceType) throws MelodyAudioException {

        Objects.requireNonNull(audioSource);
        Objects.requireNonNull(sourceType);

        //OpenAL sources should get generated in the main thread, so make sure we're in the correct thread
        RenderSystem.assertOnRenderThread();

        if (!ALUtils.isOpenAlReady()) {
            throw new MelodyAudioException("Failed to create OGG audio clip! OpenAL not ready! Audio source: " + audioSource);
        }

        ALAudioClip clip = ALAudioClip.create();
        if (clip == null) throw new MelodyAudioException("Failed to create OGG audio clip! Clip was NULL for: " + audioSource);

        if (sourceType == SourceType.RESOURCE_LOCATION) {
            ResourceLocation location = ResourceLocation.tryParse(audioSource);
            if (location == null) {
                clip.closeQuietly();
                throw new MelodyAudioException("Failed to create OGG audio clip! ResourceLocation parsing failed: " + audioSource);
            }
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isPresent()) {
                try {
                    final InputStream in = resource.get().open();
                    CompletableFuture<ALAudioClip> completableFuture = new CompletableFuture<>();
                    new Thread(() -> {
                        Exception ex = tryCreateAndSetOggStaticBuffer(clip, in);
                        if (ex != null) {
                            completableFuture.completeExceptionally(ex);
                            clip.closeQuietly();
                        } else {
                            completableFuture.complete(clip);
                        }
                    }).start();
                    return completableFuture;
                } catch (Exception ex) {
                    clip.closeQuietly();
                    throw (MelodyAudioException) new MelodyAudioException("Failed to create OGG audio clip! Failed to open ResourceLocation input stream: " + audioSource).initCause(ex);
                }
            } else {
                clip.closeQuietly();
                throw new MelodyAudioException("Failed to create OGG audio clip! Resource for ResourceLocation not found: " + audioSource);
            }
        }
        else if (sourceType == SourceType.LOCAL_FILE) {
            File file = new File(audioSource);
            if (!file.isFile()) {
                clip.closeQuietly();
                throw new MelodyAudioException("Failed to create OGG audio clip! File not found: " + audioSource);
            }
            try {
                final InputStream in = new FileInputStream(file);
                CompletableFuture<ALAudioClip> completableFuture = new CompletableFuture<>();
                new Thread(() -> {
                    Exception ex = tryCreateAndSetOggStaticBuffer(clip, in);
                    if (ex != null) {
                        completableFuture.completeExceptionally(ex);
                        clip.closeQuietly();
                    } else {
                        completableFuture.complete(clip);
                    }
                }).start();
                return completableFuture;
            } catch (Exception ex) {
                clip.closeQuietly();
                throw (MelodyAudioException) new MelodyAudioException("Failed to create OGG audio clip! Failed to open File input stream: " + audioSource).initCause(ex);
            }
        }
        else { //WEB
            if (!BASIC_URL_TEXT_VALIDATOR.get(audioSource)) {
                clip.closeQuietly();
                throw new MelodyAudioException("Failed to create OGG audio clip! Invalid URL: " + audioSource);
            }
            try {
                final InputStream webIn = openWebResourceStream(audioSource);
                CompletableFuture<ALAudioClip> completableFuture = new CompletableFuture<>();
                new Thread(() -> {
                    ByteArrayInputStream in = null;
                    Exception streamReadException = null;
                    try {
                        in = new ByteArrayInputStream(webIn.readAllBytes());
                    } catch (Exception ex) {
                        streamReadException = ex;
                    }
                    IOUtils.closeQuietly(webIn);
                    if (in == null) {
                        completableFuture.completeExceptionally(streamReadException);
                        clip.closeQuietly();
                    } else {
                        Exception ex = tryCreateAndSetOggStaticBuffer(clip, in);
                        if (ex != null) {
                            completableFuture.completeExceptionally(ex);
                            clip.closeQuietly();
                        } else {
                            completableFuture.complete(clip);
                        }
                    }
                }).start();
                return completableFuture;
            } catch (Exception ex) {
                clip.closeQuietly();
                throw (MelodyAudioException) new MelodyAudioException("Failed to create OGG audio clip! Failed to open web input stream: " + audioSource).initCause(ex);
            }
        }

    }

    @NotNull
    public static CompletableFuture<ALAudioClip> wav(@NotNull String audioSource, @NotNull SourceType sourceType) throws MelodyAudioException {

        Objects.requireNonNull(audioSource);
        Objects.requireNonNull(sourceType);

        //OpenAL sources should get generated in the main thread, so make sure we're in the correct thread
        RenderSystem.assertOnRenderThread();

        if (!ALUtils.isOpenAlReady()) {
            throw new MelodyAudioException("Failed to create WAV audio clip! OpenAL not ready! Audio source: " + audioSource);
        }

        ALAudioClip clip = ALAudioClip.create();
        if (clip == null) throw new MelodyAudioException("Failed to create WAV audio clip! Clip was NULL for: " + audioSource);

        if (sourceType == SourceType.RESOURCE_LOCATION) {
            ResourceLocation location = ResourceLocation.tryParse(audioSource);
            if (location == null) {
                clip.closeQuietly();
                throw new MelodyAudioException("Failed to create WAV audio clip! ResourceLocation parsing failed: " + audioSource);
            }
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isPresent()) {
                try {
                    final InputStream in = resource.get().open();
                    CompletableFuture<ALAudioClip> completableFuture = new CompletableFuture<>();
                    new Thread(() -> {
                        Exception ex = tryCreateAndSetWavStaticBuffer(clip, in);
                        if (ex != null) {
                            completableFuture.completeExceptionally(ex);
                            clip.closeQuietly();
                        } else {
                            completableFuture.complete(clip);
                        }
                    }).start();
                    return completableFuture;
                } catch (Exception ex) {
                    clip.closeQuietly();
                    throw (MelodyAudioException) new MelodyAudioException("Failed to create WAV audio clip! Failed to open ResourceLocation input stream: " + audioSource).initCause(ex);
                }
            } else {
                clip.closeQuietly();
                throw new MelodyAudioException("Failed to create WAV audio clip! Resource for ResourceLocation not found: " + audioSource);
            }
        }
        else if (sourceType == SourceType.LOCAL_FILE) {
            File file = new File(audioSource);
            if (!file.isFile()) {
                clip.closeQuietly();
                throw new MelodyAudioException("Failed to create WAV audio clip! File not found: " + audioSource);
            }
            try {
                final InputStream in = new FileInputStream(file);
                CompletableFuture<ALAudioClip> completableFuture = new CompletableFuture<>();
                new Thread(() -> {
                    Exception ex = tryCreateAndSetWavStaticBuffer(clip, in);
                    if (ex != null) {
                        completableFuture.completeExceptionally(ex);
                        clip.closeQuietly();
                    } else {
                        completableFuture.complete(clip);
                    }
                }).start();
                return completableFuture;
            } catch (Exception ex) {
                clip.closeQuietly();
                throw (MelodyAudioException) new MelodyAudioException("Failed to create WAV audio clip! Failed to open File input stream: " + audioSource).initCause(ex);
            }
        }
        else { https://www.youtube.com/watch?v=niNDSimJ1Ds
            if (!BASIC_URL_TEXT_VALIDATOR.get(audioSource)) {
                clip.closeQuietly();
                throw new MelodyAudioException("Failed to create WAV audio clip! Invalid URL: " + audioSource);
            }
            try {
                final InputStream webIn = openWebResourceStream(audioSource);
                CompletableFuture<ALAudioClip> completableFuture = new CompletableFuture<>();
                new Thread(() -> {
                    Exception ex = tryCreateAndSetWavStaticBuffer(clip, webIn);
                    if (ex != null) {
                        completableFuture.completeExceptionally(ex);
                        clip.closeQuietly();
                    } else {
                        completableFuture.complete(clip);
                    }
                }).start();
                return completableFuture;
            } catch (Exception ex) {
                clip.closeQuietly();
                throw (MelodyAudioException) new MelodyAudioException("Failed to create WAV audio clip! Failed to open web input stream: " + audioSource).initCause(ex);
            }
        }

    }

    @Nullable
    private static Exception tryCreateAndSetOggStaticBuffer(@NotNull ALAudioClip setTo, @NotNull InputStream in) {
        OggAudioStream stream = null;
        Exception exception = null;
        try {
            stream = new OggAudioStream(in);
            ByteBuffer byteBuffer = stream.readAll();
            ALAudioBuffer audioBuffer = new ALAudioBuffer(byteBuffer, stream.getFormat());
            setTo.setStaticBuffer(audioBuffer);
        } catch (Exception ex) {
            exception = ex;
        }
        IOUtils.closeQuietly(stream);
        IOUtils.closeQuietly(in);
        return exception;
    }

    @Nullable
    private static Exception tryCreateAndSetWavStaticBuffer(@NotNull ALAudioClip setTo, @NotNull InputStream in) {
        AudioInputStream stream = null;
        ByteArrayInputStream byteIn = null;
        Exception exception = null;
        try {
            //Needed because otherwise getAudioInputStream() could fail due to issues like "in" not supporting mark/reset, etc.
            byteIn = new ByteArrayInputStream(in.readAllBytes());
            stream = AudioSystem.getAudioInputStream(byteIn);
            ByteBuffer byteBuffer = ALUtils.readStreamIntoBuffer(stream);
            ALAudioBuffer audioBuffer = new ALAudioBuffer(byteBuffer, stream.getFormat());
            setTo.setStaticBuffer(audioBuffer);
        } catch (Exception ex) {
            exception = ex;
        }
        IOUtils.closeQuietly(stream);
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(byteIn);
        return exception;
    }

    @NotNull
    private static InputStream openWebResourceStream(@NotNull String resourceURL) throws IOException {
        URL actualURL = new URL(resourceURL);
        HttpURLConnection connection = (HttpURLConnection)actualURL.openConnection();
        connection.addRequestProperty("User-Agent", "Mozilla/4.0");
        return connection.getInputStream();
    }

    public enum SourceType {
        RESOURCE_LOCATION,
        LOCAL_FILE,
        https://www.youtube.com/watch?v=niNDSimJ1Ds
    }

    @FunctionalInterface
    private interface ConsumingSupplier<C, R> {
        R get(C consumes);
    }

}
