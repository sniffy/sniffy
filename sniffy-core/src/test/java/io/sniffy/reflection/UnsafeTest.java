package io.sniffy.reflection;

import io.sniffy.reflection.field.UnresolvedNonStaticFieldRef;
import org.junit.Test;

import static io.sniffy.reflection.Unsafe.$;
import static org.junit.Assert.*;

public class UnsafeTest {

    @Test
    public void testGetSunMiscUnsafe() {
        assertNotNull(Unsafe.getSunMiscUnsafe());
    }

    @Test
    public void testModifyPrivateFields() throws Exception {

        Object objectField = new Object();
        int intField = 42;
        boolean booleanField = false;

        ClassWithDifferentFields objectWithDifferentFields = new ClassWithDifferentFields(objectField, intField, booleanField);

        UnresolvedNonStaticFieldRef<ClassWithDifferentFields, Object> privateObjectFieldRef =
                $(ClassWithDifferentFields.class).getNonStaticField("privateObjectField");

        assertNotNull(privateObjectFieldRef);

        assertEquals(objectField, privateObjectFieldRef.get(objectWithDifferentFields));

        objectField = new Object();
        privateObjectFieldRef.set(objectWithDifferentFields, objectField);
        assertEquals(objectField, privateObjectFieldRef.get(objectWithDifferentFields));
    }

    @Test
    public void testCompareAndSetBooleanFieldFailure() throws Exception {

        Object objectField = new Object();
        int intField = 42;
        boolean booleanField = false;

        ClassWithDifferentFields objectWithDifferentFields = new ClassWithDifferentFields(objectField, intField, booleanField);

        UnresolvedNonStaticFieldRef<ClassWithDifferentFields, Boolean> privateBooleanFieldRef =
                $(ClassWithDifferentFields.class).getNonStaticField("privateBooleanField");

        assertNotNull(privateBooleanFieldRef);

        assertFalse(privateBooleanFieldRef.compareAndSet(objectWithDifferentFields, true, true));
        assertFalse(privateBooleanFieldRef.get(objectWithDifferentFields));
    }

    @Test
    public void testCompareAndSetBooleanFieldSuccess() throws Exception {

        Object objectField = new Object();
        int intField = 42;
        boolean booleanField = false;

        ClassWithDifferentFields objectWithDifferentFields = new ClassWithDifferentFields(objectField, intField, booleanField);

        UnresolvedNonStaticFieldRef<ClassWithDifferentFields, Boolean> privateBooleanFieldRef =
                $(ClassWithDifferentFields.class).getNonStaticField("privateBooleanField");

        assertNotNull(privateBooleanFieldRef);

        assertTrue(privateBooleanFieldRef.compareAndSet(objectWithDifferentFields, false, true));
        assertTrue(privateBooleanFieldRef.get(objectWithDifferentFields));
    }

    @Test
    public void testBooleanReflection() throws Exception {

        JavaClassWithManyBooleanFields obj = new JavaClassWithManyBooleanFields();

        obj.field5 = true;

        $(JavaClassWithManyBooleanFields.class).getNonStaticField("field2").set(obj, true);

        assertTrue(obj.field2);
        assertTrue($(JavaClassWithManyBooleanFields.class).<Boolean>getNonStaticField("field2").get(obj));

        $(JavaClassWithManyBooleanFields.class).getNonStaticField("field1").compareAndSet(obj, false, true);

        assertTrue(obj.field1);
        assertTrue($(JavaClassWithManyBooleanFields.class).<Boolean>getNonStaticField("field1").get(obj));

        assertTrue($(JavaClassWithManyBooleanFields.class).<Boolean>getNonStaticField("field2").get(obj));

        assertFalse(obj.field3);
        assertFalse($(JavaClassWithManyBooleanFields.class).<Boolean>getNonStaticField("field3").get(obj));
    }

    @Test
    public void testModifyPrivateFinalFields() throws Exception {

        Object objectField = new Object();
        int intField = 42;
        boolean booleanField = false;

        ClassWithDifferentFinalFields objectWithDifferentFields = new ClassWithDifferentFinalFields(objectField, intField, booleanField);

        UnresolvedNonStaticFieldRef<ClassWithDifferentFinalFields, Object> privateObjectFieldRef =
                $(ClassWithDifferentFinalFields.class).getNonStaticField("privateObjectField");

        assertNotNull(privateObjectFieldRef);

        assertEquals(objectField, privateObjectFieldRef.get(objectWithDifferentFields));

        objectField = new Object();
        privateObjectFieldRef.set(objectWithDifferentFields, objectField);
        assertEquals(objectField, privateObjectFieldRef.get(objectWithDifferentFields));
    }

}