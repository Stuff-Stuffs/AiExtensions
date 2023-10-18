package io.github.stuff_stuffs.aiex.common.impl.util;

import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import io.github.stuff_stuffs.aiex.common.api.util.StringTemplate;

public class NoopSpannedLoggerImpl implements SpannedLogger {
    @Override
    public SpannedLogger open(final String span) {
        return this;
    }

    @Override
    public SpannedLogger open(final StringTemplate template, final Object... args) {
        return this;
    }

    @Override
    public String spanMessage() {
        return "";
    }

    @Override
    public void log(final Level level, final String message) {

    }

    @Override
    public void log(final Level level, final StringTemplate template, final Object... args) {

    }

    @Override
    public void close() {

    }
}
