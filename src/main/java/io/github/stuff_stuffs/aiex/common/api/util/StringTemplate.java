package io.github.stuff_stuffs.aiex.common.api.util;

import io.github.stuff_stuffs.aiex.common.impl.util.StringTemplateImpl;

public interface StringTemplate {
    int argCount();

    String apply(Object... args);

    static StringTemplate create(final String template) {
        return new StringTemplateImpl(template);
    }
}
