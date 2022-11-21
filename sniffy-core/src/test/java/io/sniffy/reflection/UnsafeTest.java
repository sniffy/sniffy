package io.sniffy.reflection;

import io.sniffy.reflection.constructor.ZeroArgsConstructorRef;
import io.sniffy.reflection.field.FieldRef;
import org.junit.Ignore;
import org.junit.Test;

import static io.sniffy.reflection.Unsafe.$;
import static org.junit.Assert.*;

public class UnsafeTest {

    @Test
    public void testGetSunMiscUnsafe() {
        assertNotNull(Unsafe.getSunMiscUnsafe());
    }

    @Test
    public void testModifyPrivateFields() throws UnsafeException {

        Object objectField = new Object();
        int intField = 42;
        boolean booleanField = false;

        ClassWithDifferentFields objectWithDifferentFields = new ClassWithDifferentFields(objectField, intField, booleanField);

        FieldRef<ClassWithDifferentFields, Object> privateObjectFieldRef =
                $(ClassWithDifferentFields.class).field("privateObjectField");

        assertNotNull(privateObjectFieldRef);

        assertEquals(objectField, privateObjectFieldRef.get(objectWithDifferentFields));

        objectField = new Object();
        privateObjectFieldRef.set(objectWithDifferentFields, objectField);
        assertEquals(objectField, privateObjectFieldRef.get(objectWithDifferentFields));
    }

    @Test
    public void testCompareAndSetBooleanFieldFailure() throws UnsafeException {

        Object objectField = new Object();
        int intField = 42;
        boolean booleanField = false;

        ClassWithDifferentFields objectWithDifferentFields = new ClassWithDifferentFields(objectField, intField, booleanField);

        FieldRef<ClassWithDifferentFields, Boolean> privateBooleanFieldRef =
                $(ClassWithDifferentFields.class).field("privateBooleanField");

        assertNotNull(privateBooleanFieldRef);

        assertFalse(privateBooleanFieldRef.compareAndSet(objectWithDifferentFields, true, true));
        assertFalse(privateBooleanFieldRef.get(objectWithDifferentFields));
    }

    @Test
    public void testCompareAndSetBooleanFieldSuccess() throws UnsafeException {

        Object objectField = new Object();
        int intField = 42;
        boolean booleanField = false;

        ClassWithDifferentFields objectWithDifferentFields = new ClassWithDifferentFields(objectField, intField, booleanField);

        FieldRef<ClassWithDifferentFields, Boolean> privateBooleanFieldRef =
                $(ClassWithDifferentFields.class).field("privateBooleanField");

        assertNotNull(privateBooleanFieldRef);

        assertTrue(privateBooleanFieldRef.compareAndSet(objectWithDifferentFields, false, true));
        assertTrue(privateBooleanFieldRef.get(objectWithDifferentFields));
    }

    @Test
    public void testBooleanReflection() throws UnsafeException {

        JavaClassWithManyBooleanFields obj = new JavaClassWithManyBooleanFields();

        obj.field5 = true;

        $(JavaClassWithManyBooleanFields.class).field("field2").set(obj, true);

        assertTrue(obj.field2);
        assertTrue($(JavaClassWithManyBooleanFields.class).<Boolean>field("field2").get(obj));

        $(JavaClassWithManyBooleanFields.class).field("field1").compareAndSet(obj, false, true);

        assertTrue(obj.field1);
        assertTrue($(JavaClassWithManyBooleanFields.class).<Boolean>field("field1").get(obj));

        assertTrue($(JavaClassWithManyBooleanFields.class).<Boolean>field("field2").get(obj));

        assertFalse(obj.field3);
        assertFalse($(JavaClassWithManyBooleanFields.class).<Boolean>field("field3").get(obj));
    }

    @Test
    public void testModifyPrivateFinalFields() throws UnsafeException {

        Object objectField = new Object();
        int intField = 42;
        boolean booleanField = false;

        ClassWithDifferentFinalFields objectWithDifferentFields = new ClassWithDifferentFinalFields(objectField, intField, booleanField);

        FieldRef<ClassWithDifferentFinalFields, Object> privateObjectFieldRef =
                $(ClassWithDifferentFinalFields.class).field("privateObjectField");

        assertNotNull(privateObjectFieldRef);

        assertEquals(objectField, privateObjectFieldRef.get(objectWithDifferentFields));

        objectField = new Object();
        privateObjectFieldRef.set(objectWithDifferentFields, objectField);
        assertEquals(objectField, privateObjectFieldRef.get(objectWithDifferentFields));
    }

    private static int counter = 0;

    private static class TestClass {

        public TestClass() {
            counter++;
        }
    }

    @Test
    @Ignore("Constructor magic doesn't work unless we introduce multi-release JARs")
    public void testConstructorInvocation() throws Exception {

        TestClass tc = new TestClass();

        ZeroArgsConstructorRef<TestClass> constructor = $(TestClass.class).constructor();
        constructor.invoke(tc);
        constructor.invoke(tc);
        constructor.invoke(tc);

        assertEquals(4, counter);

    }

    @Test
    public void testGenerics() throws Exception {

        Object object = 42L;

        Number number = 42L;

        Long longNumber = 42L;

        //noinspection unchecked
        ClassRef<Number> cr1 = $((Class<Number>) number.getClass());
        ClassRef<Number> cr2 = $(number.getClass(), Number.class);
        ClassRef<Number> cr3 = $(Number.class);

        cr1.superClassRef(Object.class).method(String.class, "toString").invoke(object);
        cr2.superClassRef(Object.class).method(String.class, "toString").invoke(object);
        cr3.superClassRef(Object.class).method(String.class, "toString").invoke(object);

        cr1.cast(Object.class).method(String.class, "toString").invoke(object);
        cr2.cast(Object.class).method(String.class, "toString").invoke(object);
        cr3.cast(Object.class).method(String.class, "toString").invoke(object);

        cr1.method(String.class, "toString").invoke(number);
        cr2.method(String.class, "toString").invoke(number);
        cr3.method(String.class, "toString").invoke(number);

        cr1.method(String.class, "toString").invoke(longNumber);
        cr2.method(String.class, "toString").invoke(longNumber);
        cr3.method(String.class, "toString").invoke(longNumber);

        {
            cr1.method(Integer.TYPE, "intValue").invoke(number);
            cr1.method(Integer.TYPE, "intValue").invoke(number);
            cr1.cast(Object.class).method(Integer.TYPE, "intValue").invoke(number);
            try {
                cr1.superClassRef(Object.class).method(Integer.TYPE, "intValue").invoke(number);
                fail("Should have thrown an exception");
            } catch (Exception e) {
                assertTrue(e instanceof NoSuchMethodException);
            }
        }

    }

}