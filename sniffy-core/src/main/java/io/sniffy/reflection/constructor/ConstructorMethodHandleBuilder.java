package io.sniffy.reflection.constructor;

import io.sniffy.reflection.UnresolvedRefException;
import io.sniffy.reflection.UnsafeInvocationException;
import io.sniffy.reflection.clazz.ClassRef;
import io.sniffy.reflection.field.UnresolvedNonStaticFieldRef;
import io.sniffy.reflection.field.UnresolvedStaticFieldRef;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static io.sniffy.reflection.Unsafe.$;

/**
 * <a href="https://stackoverflow.com/questions/48616630/is-it-possible-to-call-constructor-on-existing-instance">See stackoverflow discussion</a>
 * TODO: doesn't compile on both Java 8 and higher - consider using multi version jars
 */
public class ConstructorMethodHandleBuilder {

    public static MethodHandle constructorMethodHandle(Class<?> clazz) throws UnresolvedRefException, UnsafeInvocationException, NoSuchMethodException, IllegalAccessException {
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
        ClassRef<MethodHandles.Lookup> lookupClassRef = $(MethodHandles.Lookup.class);
        UnresolvedStaticFieldRef<MethodHandles.Lookup> implLookupFieldRef = lookupClassRef.getStaticField("IMPL_LOOKUP");
        MethodHandles.Lookup implLookup = implLookupFieldRef.get();

        MethodType constructorMethodType = MethodType.methodType(Void.TYPE);
        MethodHandle constructor = implLookup.findConstructor(clazz, constructorMethodType);

        UnresolvedNonStaticFieldRef<Object, Object> initMethodFieldRef = $("java.lang.invoke.DirectMethodHandle$Constructor").getNonStaticField("initMethod");
        /* MemberName */ Object initMemberName = initMethodFieldRef.get(constructor);

        UnresolvedNonStaticFieldRef<Object, Integer> memberNameFlagsFieldRef = $("java.lang.invoke.MemberName").getNonStaticField("flags");
        int flags = memberNameFlagsFieldRef.get(initMemberName);
        flags &= ~0x00020000; // remove "is constructor"
        flags |= 0x00010000; // add "is (non-constructor) method"

        memberNameFlagsFieldRef.set(initMemberName, flags);

        MethodHandle handle = null;

            /*if (JVMUtil.getVersion() > 8) {
                //noinspection unchecked
                handle = $(MethodHandles.Lookup.class).method(MethodHandle.class, "getDirectMethod",
                                Byte.TYPE, Class.class, (Class<Object>) Class.forName("java.lang.invoke.MemberName"), MethodHandles.Lookup.class).
                        invoke(
                                implLookup, (byte) 5, clazz, initMemberName, implLookup
                        );

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

            } else {

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

            }*/

        return handle;

    }

}
