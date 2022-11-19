package io.sniffy.reflection;

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
                $(ClassWithDifferentFields.class, "privateObjectField");

        assertNotNull(privateObjectFieldRef);

        assertEquals(objectField, privateObjectFieldRef.getValue(objectWithDifferentFields));

        objectField = new Object();
        privateObjectFieldRef.setValue(objectWithDifferentFields, objectField);
        assertEquals(objectField, privateObjectFieldRef.getValue(objectWithDifferentFields));
    }

    @Test
    public void testModifyPrivateFinalFields() throws UnsafeException {

        Object objectField = new Object();
        int intField = 42;
        boolean booleanField = false;

        ClassWithDifferentFinalFields objectWithDifferentFields = new ClassWithDifferentFinalFields(objectField, intField, booleanField);

        FieldRef<ClassWithDifferentFinalFields, Object> privateObjectFieldRef =
                $(ClassWithDifferentFinalFields.class, "privateObjectField");

        assertNotNull(privateObjectFieldRef);

        assertEquals(objectField, privateObjectFieldRef.getValue(objectWithDifferentFields));

        objectField = new Object();
        privateObjectFieldRef.setValue(objectWithDifferentFields, objectField);
        assertEquals(objectField, privateObjectFieldRef.getValue(objectWithDifferentFields));
    }

}