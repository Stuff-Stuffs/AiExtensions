package io.github.stuff_stuffs.aiex.common.impl.util;

import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import io.github.stuff_stuffs.aiex.common.api.util.StringTemplate;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class SpannedLoggerImpl implements SpannedLogger {
    private final Level level;
    private final OutputStream stream;
    private final String prefix;
    private final ObjectArrayList<Span> path = new ObjectArrayList<>();
    private boolean closed = false;

    public SpannedLoggerImpl(final Level level, final OutputStream stream, final String prefix) {
        this.level = level;
        this.stream = stream;
        this.prefix = prefix;
    }

    @Override
    public SpannedLogger open(final String span) {
        return open(() -> span, 0);
    }

    @Override
    public SpannedLogger open(final StringTemplate template, final Object... args) {
        return open(() -> template.apply(args), 0);
    }

    public SpannedLogger open(final Supplier<String> span, final int depth) {
        if (closed) {
            throw new IllegalStateException();
        }
        if (depth != path.size()) {
            throw new IllegalStateException();
        }
        path.add(new Span(span));
        return new Child(this, path.size());
    }

    @Override
    public String spanMessage() {
        return prefix;
    }

    private void writePrelude() {
        int i = path.size() - 1;
        while (i >= 0) {
            final Span span = path.get(i--);
            if (span.opened) {
                return;
            }
            try {
                stream.write(("<" + span + ">").getBytes(StandardCharsets.UTF_8));
                span.opened = true;
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void log(final Level level, final String message) {
        if (closed) {
            throw new IllegalStateException();
        }
        if (this.level.compareTo(level) >= 0) {
            writePrelude();
            try {
                stream.write(("<message " + " level=" + level.name() + " >\n").getBytes(StandardCharsets.UTF_8));
                stream.write(message.getBytes(StandardCharsets.UTF_8));
                stream.write('\n');
                stream.write("</message>".getBytes(StandardCharsets.UTF_8));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void log(final Level level, final StringTemplate template, final Object... args) {
        if (closed) {
            throw new IllegalStateException();
        }
        if (this.level.compareTo(level) >= 0) {
            log(level, template.apply(args));
        }
    }

    @Override
    public void close() {
        if (closed) {
            throw new IllegalStateException();
        }
        try {
            try {
                while (!path.isEmpty()) {
                    pop();
                }
            } catch (final Exception ignored) {
            }
            stream.write(("</" + prefix + ">").getBytes(StandardCharsets.UTF_8));
            stream.close();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        closed = true;
    }

    public void pop() {
        final Span p = path.pop();
        if (p.opened) {
            try {
                stream.write(("</" + p.message + ">").getBytes(StandardCharsets.UTF_8));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class Child implements SpannedLogger {
        private final SpannedLoggerImpl parent;
        private final int depth;
        private boolean closed = false;

        private Child(final SpannedLoggerImpl parent, final int depth) {
            this.parent = parent;
            this.depth = depth;
        }

        private boolean closed() {
            return closed || parent.closed;
        }

        @Override
        public SpannedLogger open(final String span) {
            if (closed()) {
                throw new IllegalStateException();
            }
            return parent.open(() -> span, depth);
        }

        @Override
        public SpannedLogger open(final StringTemplate template, final Object... args) {
            return parent.open(() -> template.apply(args), depth);
        }

        @Override
        public String spanMessage() {
            if (closed()) {
                throw new IllegalStateException();
            }
            return parent.path.get(depth).getMessage();
        }

        @Override
        public void log(final Level level, final String message) {
            if (closed() || parent.path.size() != depth) {
                throw new IllegalStateException();
            }
            parent.log(level, message);
        }

        @Override
        public void log(final Level level, final StringTemplate template, final Object... args) {
            if (closed() || parent.path.size() != depth) {
                throw new IllegalStateException();
            }
            parent.log(level, template, args);
        }

        @Override
        public void close() {
            if (closed || parent.path.size() != depth) {
                throw new IllegalStateException();
            }
            closed = true;
            if (!parent.closed) {
                parent.pop();
            }
        }
    }

    private static final class Span {
        private final Supplier<String> message;
        private String cachedMessage = null;
        private boolean opened;

        private Span(final Supplier<String> message) {
            this.message = message;
            opened = false;
        }

        public String getMessage() {
            if (cachedMessage == null) {
                cachedMessage = message.get();
            }
            return cachedMessage;
        }
    }
}
