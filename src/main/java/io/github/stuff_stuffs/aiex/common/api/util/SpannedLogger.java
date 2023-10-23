package io.github.stuff_stuffs.aiex.common.api.util;

import io.github.stuff_stuffs.aiex.common.impl.util.NoopSpannedLoggerImpl;
import io.github.stuff_stuffs.aiex.common.impl.util.SpannedLoggerImpl;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

public interface SpannedLogger extends AutoCloseable {
    SpannedLogger open(String span);

    SpannedLogger open(StringTemplate template, Object... args);

    String spanMessage();

    default void error(final String message) {
        log(Level.ERROR, message);
    }

    default void warning(final String message) {
        log(Level.WARNING, message);
    }

    default void debug(final String message) {
        log(Level.DEBUG, message);
    }

    void log(Level level, String message);

    default void error(final StringTemplate template, final Object... args) {
        log(Level.ERROR, template, args);
    }

    default void warning(final StringTemplate template, final Object... args) {
        log(Level.WARNING, template, args);
    }

    default void debug(final StringTemplate template, final Object... args) {
        log(Level.DEBUG, template, args);
    }

    void log(Level level, StringTemplate template, Object... args);

    @Override
    void close();

    static SpannedLogger create(final Level level, final String prefix, final Path path) {
        try {
            return new SpannedLoggerImpl(level, new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(path))), prefix);
        } catch (final IOException e) {
            AiExCommon.LOGGER.error("Could not create spanned logger! Fallback to noop", e);
            return new NoopSpannedLoggerImpl();
        }
    }

    enum Level {
        ERROR,
        WARNING,
        DEBUG
    }
}
