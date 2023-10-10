package io.github.stuff_stuffs.aiex.common.internal;

import io.github.stuff_stuffs.aiex.common.api.entity.AiFakePlayer;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public final class FakePlayerAsm {
    private static final Loader LOADER = new Loader();
    private static final ConcurrentHashMap<Class<?>, BiFunction<ServerWorld, Entity, ? extends AiFakePlayer>> CACHE = new ConcurrentHashMap<>();
    private static final Object LOCK = new Object();

    public static <T extends Entity> AiFakePlayer get(final T entity, final ServerWorld world) {
        final Class<? extends Entity> entityClass = entity.getClass();
        BiFunction<ServerWorld, Entity, ? extends AiFakePlayer> custom = CACHE.get(entityClass);
        if (custom == null) {
            synchronized (LOCK) {
                custom = CACHE.get(entityClass);
                if (custom != null) {
                    return custom.apply(world, entity);
                }
                final Class<?> clazz = create(entityClass);

                try {
                    final MethodHandle handle = MethodHandles.lookup().unreflectConstructor(clazz.getConstructor(ServerWorld.class, entityClass));
                    final BiFunction<ServerWorld, Entity, AiFakePlayer> function = new BiFunction<>() {
                        @Override
                        public AiFakePlayer apply(final ServerWorld world, final Entity entity) {
                            try {
                                return (AiFakePlayer) handle.invoke(world, entity);
                            } catch (final Throwable e) {
                                throw new RuntimeException(e);
                            }
                        }
                    };
                    CACHE.put(entityClass, function);
                    return function.apply(world, entity);
                } catch (final IllegalAccessException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return custom.apply(world, entity);
    }

    private static <T extends Entity> Class<?> create(final Class<T> clazz) {
        final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        final ClassVisitor classVisitor = new CheckClassAdapter(classWriter);
        final String baseName = Type.getInternalName(AiFakePlayer.class);
        final String thisName = baseName + clazz.getSimpleName();
        classVisitor.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, thisName, null, baseName, null);
        final String clazzDescriptor = Type.getDescriptor(clazz);
        classVisitor.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "delegate", clazzDescriptor, null, null);
        for (final Method method : clazz.getMethods()) {
            if ((method.getModifiers() & Modifier.FINAL) == 0 && !method.isAnnotationPresent(AiFakePlayer.NoGenerateDelegate.class)) {
                final Method delegateMethod;
                try {
                    delegateMethod = clazz.getMethod(method.getName(), method.getParameterTypes());
                } catch (final NoSuchMethodException e) {
                    continue;
                }
                final Parameter[] parameters = method.getParameters();
                final MethodVisitor methodVisitor = classVisitor.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), Type.getMethodDescriptor(method), null, null);
                for (final Parameter type : parameters) {
                    methodVisitor.visitParameter(type.getName(), Opcodes.ACC_FINAL);
                }
                methodVisitor.visitCode();
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitFieldInsn(Opcodes.GETFIELD, thisName, "delegate", clazzDescriptor);
                methodVisitor.visitInsn(Opcodes.DUP);
                final Label label = new Label();
                methodVisitor.visitJumpInsn(Opcodes.IFNULL, label);
                int idx = 1;
                for (final Parameter parameter : parameters) {
                    final Type type = Type.getType(parameter.getType());
                    methodVisitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), idx);
                    idx = idx + type.getSize();
                }
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(clazz), delegateMethod.getName(), Type.getMethodDescriptor(delegateMethod), false);
                methodVisitor.visitInsn(Type.getType(delegateMethod.getReturnType()).getOpcode(Opcodes.IRETURN));
                methodVisitor.visitLabel(label);
                methodVisitor.visitInsn(Opcodes.POP);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                idx = 1;
                for (final Parameter parameter : parameters) {
                    final Type type = Type.getType(parameter.getType());
                    methodVisitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), idx);
                    idx = idx + type.getSize();
                }
                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(AiFakePlayer.class), method.getName(), Type.getMethodDescriptor(method), false);
                methodVisitor.visitInsn(Type.getType(method.getReturnType()).getOpcode(Opcodes.IRETURN));
                methodVisitor.visitMaxs(idx + 1, idx + 1);
                methodVisitor.visitEnd();
            }
        }
        final MethodVisitor methodVisitor = classVisitor.visitMethod(Opcodes.ACC_PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ServerWorld.class), Type.getType(clazz)), null, null);
        methodVisitor.visitParameter("world", Opcodes.ACC_FINAL);
        methodVisitor.visitParameter("entity", Opcodes.ACC_FINAL);
        methodVisitor.visitCode();
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, baseName, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ServerWorld.class), Type.getType(Entity.class)), false);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, thisName, "delegate", clazzDescriptor);
        methodVisitor.visitInsn(Opcodes.RETURN);
        methodVisitor.visitMaxs(3, 3);
        methodVisitor.visitEnd();
        classVisitor.visitEnd();
        return LOADER.defineClass(thisName.replace('/', '.'), classWriter.toByteArray());
    }

    private static final class Loader extends ClassLoader {
        public Loader() {
            super(FakePlayerAsm.class.getClassLoader());
        }

        public Class<?> defineClass(final String name, final byte[] data) {
            return defineClass(name, data, 0, data.length);
        }
    }

    private FakePlayerAsm() {
    }
}
