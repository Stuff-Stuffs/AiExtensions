package io.github.stuff_stuffs.aiex.common.internal;

import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReferenceDataType;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.entity.Entity;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class EntityReferenceDataTypeCache {
    private static Map<Class<?>, List<EntityReferenceDataType<?, ?>>> TYPE_CACHE = new Object2ReferenceOpenHashMap<>();
    private static final VarHandle TYPE_CACHE_HANDLE;

    static {
        try {
            TYPE_CACHE_HANDLE = MethodHandles.lookup().findStaticVarHandle(EntityReferenceDataTypeCache.class, "TYPE_CACHE", Map.class);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<EntityReferenceDataType<?, ?>> getApplicable(final Class<?> clazz) {
        if (!Entity.class.isAssignableFrom(clazz)) {
            return Collections.emptyList();
        }
        //noinspection unchecked
        Map<Class<?>, List<EntityReferenceDataType<?, ?>>> typeCache = (Map<Class<?>, List<EntityReferenceDataType<?, ?>>>) TYPE_CACHE_HANDLE.getAcquire();
        final List<EntityReferenceDataType<?, ?>> types = typeCache.get(clazz);
        if (types != null) {
            return types;
        }
        final List<EntityReferenceDataType<?, ?>> t = new ArrayList<>();
        for (final EntityReferenceDataType<?, ?> type : EntityReferenceDataType.REGISTRY) {
            if (type.applicableEntityType().isAssignableFrom(clazz)) {
                t.add(type);
            }
        }
        Map<Class<?>, List<EntityReferenceDataType<?, ?>>> copy;
        do {
            //noinspection unchecked
            typeCache = (Map<Class<?>, List<EntityReferenceDataType<?, ?>>>) TYPE_CACHE_HANDLE.getAcquire();
            copy = new Object2ReferenceOpenHashMap<>(typeCache);
            copy.put(clazz, t);
        } while (TYPE_CACHE_HANDLE.compareAndExchangeRelease(typeCache, copy) != typeCache);
        return t;
    }

    public static void clear() {
        Map<Class<?>, List<EntityReferenceDataType<?, ?>>> typeCache;
        do {
            //noinspection unchecked
            typeCache = (Map<Class<?>, List<EntityReferenceDataType<?, ?>>>) TYPE_CACHE_HANDLE.getAcquire();
        } while (TYPE_CACHE_HANDLE.compareAndExchangeRelease(typeCache, Collections.emptyMap()) != typeCache);
    }

    private EntityReferenceDataTypeCache() {
    }
}
