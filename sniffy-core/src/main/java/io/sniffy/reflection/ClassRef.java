package io.sniffy.reflection;

import io.sniffy.reflection.constructor.ZeroArgsConstructorRef;
import io.sniffy.reflection.field.FieldRef;
import io.sniffy.reflection.method.*;
import io.sniffy.util.JVMUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static io.sniffy.reflection.Unsafe.$;

public class ClassRef<C> implements ResolvableRef {

    private final Class<C> clazz;

    public ClassRef(Class<C> clazz) {
        this.clazz = clazz;
    }

    @Override
    public boolean isResolved() {
        return null != clazz;
    }

    @SuppressWarnings("Convert2Diamond")
    public <T, C1> FieldRef<? super C1, T> firstField(String fieldName) {
        try {
            Exception firstException = null;
            Class<? super C> clazz = this.clazz;
            while (clazz != Object.class) {
                try {
                    Field declaredField = clazz.getDeclaredField(fieldName);
                    return new FieldRef<C1, T>(declaredField, null);
                } catch (NoSuchFieldException e) {
                    if (null == firstException) {
                        firstException = e;
                    }
                    clazz = clazz.getSuperclass();
                }
            }
            return new FieldRef<C1, T>(null, firstException);
        } catch (Throwable e) {
            return new FieldRef<C1, T>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <T> FieldRef<C, T> field(String fieldName) {
        try {
            Field declaredField = clazz.getDeclaredField(fieldName);
            return new FieldRef<C, T>(declaredField, null);
        } catch (Throwable e) {
            return new FieldRef<C, T>(null, e);
        }
    }

    // esoteric

    @SuppressWarnings("RedundantSuppression")
    public ZeroArgsConstructorRef<C> constructor() throws UnsafeException {

        try {
            /*
             * Step 0: invoke MethodHandles.publicLookup() and ignore result
             * If not done, on certain JVMs the IMPL_LOOKUP field below might be null
             */
            //noinspection ResultOfMethodCallIgnored
            MethodHandles.publicLookup();

            /*
             * Step 1: Obtaining a trusted MethodHandles.Lookup
             * Next, a java.lang.invoke.MethodHandles$Lookup is needed to get the actual method handle for the constructor.
             * This class has a permission system which works through the allowedModes property in Lookup, which is set to a bunch of Flags. There is a special TRUSTED flag that circumvents all permission checks.
             * Unfortunately, the allowedModes field is filtered from reflection, so we cannot simply bypass the permissions by setting that value through reflection.
             * Even though reflection filters can be circumvented as well, there is a simpler way: Lookup contains a static field IMPL_LOOKUP, which holds a Lookup with those TRUSTED permissions. We can get this instance by using reflection and Unsafe:
             */
            FieldRef<MethodHandles.Lookup, MethodHandles.Lookup> implLookupFieldRef = $(MethodHandles.Lookup.class).field("IMPL_LOOKUP");
            MethodHandles.Lookup implLookup = implLookupFieldRef.get(null);

            MethodType constructorMethodType = MethodType.methodType(Void.TYPE);
            MethodHandle constructor = implLookup.findConstructor(clazz, constructorMethodType);

            FieldRef<Object, Object> initMethodFieldRef = $("java.lang.invoke.DirectMethodHandle$Constructor").field("initMethod");
            /* MemberName */ Object initMemberName = initMethodFieldRef.get(constructor);

            FieldRef<Object, Integer> memberNameFlagsFieldRef = $("java.lang.invoke.MemberName").field("flags");
            int flags = memberNameFlagsFieldRef.get(initMemberName);
            flags &= ~0x00020000; // remove "is constructor"
            flags |= 0x00010000; // add "is (non-constructor) method"

            memberNameFlagsFieldRef.set(initMemberName, flags);

            MethodHandle handle;

            if (JVMUtil.getVersion() > 8) {
                //noinspection unchecked
                handle = $(MethodHandles.Lookup.class).method(MethodHandle.class, "getDirectMethod",
                        Byte.TYPE, Class.class, (Class<Object>) Class.forName("java.lang.invoke.MemberName"), MethodHandles.Lookup.class).
                        invoke(
                                implLookup, (byte) 5, clazz, initMemberName, implLookup
                        );
                /*
                //noinspection JavaLangInvokeHandleSignatur e
                MethodHandle getDirectMethodHandle = implLookup.findVirtual(
                        MethodHandles.Lookup.class,
                        "getDirectMethod",
                        MethodType.methodType(
                                MethodHandle.class,
                                byte.class,
                                Class.class,
                                Class.forName("java.lang.invoke.MemberName"),
                                MethodHandles.Lookup.class
                        )
                );

                handle = (MethodHandle) getDirectMethodHandle.invoke(implLookup, (byte) 5, clazz, initMemberName, implLookup);
                 */
            } else {
                //noinspection unchecked
                handle = $(MethodHandles.Lookup.class).method(MethodHandle.class, "getDirectMethod",
                                Byte.TYPE, Class.class, (Class<Object>) Class.forName("java.lang.invoke.MemberName"), Class.class).
                        invoke(
                                implLookup, (byte) 5, clazz, initMemberName, MethodHandles.class
                        );
                /*
                //noinspection JavaLangInvokeHandleSignatur e
                MethodHandle getDirectMethodHandle = implLookup.findVirtual(
                        MethodHandles.Lookup.class,
                        "getDirectMethod",
                        MethodType.methodType(
                                MethodHandle.class,
                                byte.class,
                                Class.class,
                                Class.forName("java.lang.invoke.MemberName"),
                                Class.class
                        )
                );

                handle = (MethodHandle) getDirectMethodHandle.invoke(implLookup, (byte) 5, clazz, initMemberName, MethodHandles.class);
                 */
            }

            return new ZeroArgsConstructorRef<C>(handle, null);

        } catch (Throwable e) {
            e.printStackTrace();
            return new ZeroArgsConstructorRef<C>(null, e);
        }

    }

    // void method factories

    @SuppressWarnings("Convert2Diamond")
    public VoidZeroArgsMethodRef<C> method(String methodName) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new VoidZeroArgsMethodRef<C>(declaredMethod, null);
            } else {
                return new VoidZeroArgsMethodRef<C>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "() is not accessible"));
            }
        } catch (Throwable e) {
            return new VoidZeroArgsMethodRef<C>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <P1> VoidOneArgMethodRef<C, P1> method(String methodName, Class<P1> p1Class) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName, p1Class);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new VoidOneArgMethodRef<C, P1>(declaredMethod, null);
            } else {
                return new VoidOneArgMethodRef<C, P1>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "(" + p1Class + ") is not accessible"));
            }
        } catch (Throwable e) {
            return new VoidOneArgMethodRef<C, P1>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <P1, P2> VoidTwoArgsMethodRef<C, P1, P2> method(String methodName, Class<P1> p1Class, Class<P2> p2Class) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName, p1Class, p2Class);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new VoidTwoArgsMethodRef<C, P1, P2>(declaredMethod, null);
            } else {
                return new VoidTwoArgsMethodRef<C, P1, P2>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "(" + p1Class + ") is not accessible"));
            }
        } catch (Throwable e) {
            return new VoidTwoArgsMethodRef<C, P1, P2>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <P1, P2, P3> VoidThreeArgsMethodRef<C, P1, P2, P3> method(String methodName, Class<P1> p1Class, Class<P2> p2Class, Class<P3> p3Class) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName, p1Class, p2Class, p3Class);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new VoidThreeArgsMethodRef<C, P1, P2, P3>(declaredMethod, null);
            } else {
                return new VoidThreeArgsMethodRef<C, P1, P2, P3>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "(" + p1Class + ") is not accessible"));
            }
        } catch (Throwable e) {
            return new VoidThreeArgsMethodRef<C, P1, P2, P3>(null, e);
        }
    }

    // TODO: handle case of more arguments

    // non-void method factories


    @SuppressWarnings("Convert2Diamond")
    public <T> NonVoidZeroArgsMethodRef<T, C> method(@SuppressWarnings("unused") Class<T> tClass, String methodName) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new NonVoidZeroArgsMethodRef<T, C>(declaredMethod, null);
            } else {
                return new NonVoidZeroArgsMethodRef<T, C>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "() is not accessible"));
            }
        } catch (Throwable e) {
            return new NonVoidZeroArgsMethodRef<T, C>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <T, P1> NonVoidOneArgMethodRef<T, C, P1> method(@SuppressWarnings("unused") Class<T> tClass, String methodName, Class<P1> p1Class) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName, p1Class);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new NonVoidOneArgMethodRef<T, C, P1>(declaredMethod, null);
            } else {
                return new NonVoidOneArgMethodRef<T, C, P1>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "(" + p1Class + ") is not accessible"));
            }
        } catch (Throwable e) {
            return new NonVoidOneArgMethodRef<T, C, P1>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <T, P1, P2> NonVoidTwoArgsMethodRef<T, C, P1, P2> method(@SuppressWarnings("unused") Class<T> tClass, String methodName, Class<P1> p1Class, Class<P2> p2Class) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName, p1Class, p2Class);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new NonVoidTwoArgsMethodRef<T, C, P1, P2>(declaredMethod, null);
            } else {
                return new NonVoidTwoArgsMethodRef<T, C, P1, P2>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "(" + p1Class + ") is not accessible"));
            }
        } catch (Throwable e) {
            return new NonVoidTwoArgsMethodRef<T, C, P1, P2>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <T, P1, P2, P3> NonVoidThreeArgsMethodRef<T, C, P1, P2, P3> method(@SuppressWarnings("unused") Class<T> tClass, String methodName, Class<P1> p1Class, Class<P2> p2Class, Class<P3> p3Class) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName, p1Class, p2Class, p3Class);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new NonVoidThreeArgsMethodRef<T, C, P1, P2, P3>(declaredMethod, null);
            } else {
                return new NonVoidThreeArgsMethodRef<T, C, P1, P2, P3>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "(" + p1Class + ") is not accessible"));
            }
        } catch (Throwable e) {
            return new NonVoidThreeArgsMethodRef<T, C, P1, P2, P3>(null, e);
        }
    }

    @SuppressWarnings("Convert2Diamond")
    public <T, P1, P2, P3, P4> NonVoidFourArgsMethodRef<T, C, P1, P2, P3, P4> method(@SuppressWarnings("unused") Class<T> tClass, String methodName, Class<P1> p1Class, Class<P2> p2Class, Class<P3> p3Class, Class<P4> p4Class) {
        try {
            Method declaredMethod = clazz.getDeclaredMethod(methodName, p1Class, p2Class, p3Class, p4Class);
            if (Unsafe.setAccessible(declaredMethod)) {
                return new NonVoidFourArgsMethodRef<T, C, P1, P2, P3, P4>(declaredMethod, null);
            } else {
                return new NonVoidFourArgsMethodRef<T, C, P1, P2, P3, P4>(null, new UnsafeException("Method " + clazz.getName() + "." + methodName + "(" + p1Class + ") is not accessible"));
            }
        } catch (Throwable e) {
            return new NonVoidFourArgsMethodRef<T, C, P1, P2, P3, P4>(null, e);
        }
    }

    // TODO: handle case of more arguments

}
