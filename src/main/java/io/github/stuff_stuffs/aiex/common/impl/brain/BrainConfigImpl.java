package io.github.stuff_stuffs.aiex.common.impl.brain;

import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.util.Identifier;

import java.util.Map;

public class BrainConfigImpl implements BrainConfig {
    private final Map<Identifier, Object> values;

    public BrainConfigImpl(final Map<Identifier, Object> values) {
        this.values = values;
    }

    @Override
    public <T> T get(final Key<T> key) {
        final Object o = values.get(Key.getId(key));
        if (o == null) {
            return key.defaultValue();
        }
        if (key.clazz().isInstance(o)) {
            //noinspection unchecked
            return (T) o;
        }
        AiExCommon.LOGGER.error("Config key type mismatch!");
        return key.defaultValue();
    }

    public static final class BuilderImpl implements Builder {
        private final Map<Identifier, Object> values = new Object2ReferenceOpenHashMap<>();


        @Override
        public <T> T get(final Key<T> key) {
            final Object o = values.get(Key.getId(key));
            if (o == null) {
                return key.defaultValue();
            }
            if (key.clazz().isInstance(o)) {
                //noinspection unchecked
                return (T) o;
            }
            AiExCommon.LOGGER.error("Config key type mismatch!");
            return key.defaultValue();
        }

        @Override
        public <T> void set(final Key<T> key, final T value) {
            final Identifier id = Key.getId(key);
            if (key.defaultValue().equals(values.get(id))) {
                values.remove(id);
            } else {
                values.put(id, value);
            }
        }

        @Override
        public BrainConfig build() {
            return new BrainConfigImpl(new Object2ReferenceOpenHashMap<>(values));
        }
    }
}
