package io.github.stuff_stuffs.aiex.common.impl.util;

import io.github.stuff_stuffs.aiex.common.api.util.StringTemplate;

import java.util.ArrayList;
import java.util.List;

public class StringTemplateImpl implements StringTemplate {
    private final String[] sections;
    private final int sectionsLength;

    public StringTemplateImpl(final String template) {
        final int len = template.length();
        int i = 0;
        final List<String> strings = new ArrayList<>();
        boolean opening = false;
        int lastOpen = -1;
        while (i < len) {
            final char c = template.charAt(i);
            if (opening && c == '}') {
                if (lastOpen == i - 2) {
                    strings.add("");
                }
                strings.add(template.substring(lastOpen + 1, i - 1));
                lastOpen = i;
                opening = false;
            } else {
                opening = c == '{';
            }
            i++;
        }
        if (lastOpen + 1 == len) {
            strings.add("");
        }
        sections = strings.toArray(new String[0]);
        int sum = 0;
        for (final String section : sections) {
            sum = sum + section.length();
        }
        sectionsLength = sum;
    }

    @Override
    public int argCount() {
        return sections.length - 1;
    }

    @Override
    public String apply(final Object... args) {
        if (args.length != sections.length - 1) {
            throw new IllegalArgumentException("Must supply " + (sections.length - 1) + "arguments, found " + args.length);
        }
        final String[] strings = new String[args.length];
        int i = 0;
        int lengthSum = sectionsLength;
        for (final Object arg : args) {
            final String s = arg.toString();
            strings[i++] = s;
            lengthSum = lengthSum + s.length();
        }
        final StringBuilder buffer = new StringBuilder(lengthSum);
        buffer.append(sections[0]);
        for (int j = 0; j < args.length; j++) {
            buffer.append(strings[j]);
            buffer.append(sections[j + 1]);
        }
        return buffer.toString();
    }
}
