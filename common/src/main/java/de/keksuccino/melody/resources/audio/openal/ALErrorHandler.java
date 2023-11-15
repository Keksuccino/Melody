package de.keksuccino.melody.resources.audio.openal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.openal.AL10;

public class ALErrorHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Use this after performing an OpenAL action to check if an error happened during the action.
     *
     * @return a description of the error or NULL if no error was detected.
     */
    @Nullable
    public static String getOpenAlError() {
        int errorResult = AL10.alGetError();
        return (errorResult != 0) ? AL10.alGetString(errorResult) : null;
    }

    /**
     * Checks for OpenAL errors after performing an OpenAL action and throws
     * an {@link ALException} with error description if OpenAL detected an error.
     */
    public static void checkOpenAlError() throws ALException {
        String error = getOpenAlError();
        if (error != null) throw new ALException(error);
    }

    /**
     * Checks for OpenAL errors after performing an OpenAL action and prints the error without throwing any {@link Exception}s.
     */
    public static boolean checkAndPrintOpenAlError() {
        String error = getOpenAlError();
        if (error != null) {
            LOGGER.error("Error while handling OpenAL audio!", new ALException(error));
            return true;
        }
        return false;
    }

}
