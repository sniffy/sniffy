package io.sniffy.util;

import io.sniffy.reflection.ClassRef;
import io.sniffy.reflection.field.FieldRef;
import org.junit.Test;

import static io.sniffy.reflection.Unsafe.$;
import static org.junit.Assert.*;

public class ReflectionFieldCopierTest {

    private class Bean {

        private String foo;

        public Bean(String foo) {
            this.foo = foo;
        }

        public Bean() {
        }
    }

    @Test
    public void availableField() throws Exception {

        ClassRef<Bean> classRef = $(Bean.class);
        FieldRef<Bean, Object> fooFieldRef = classRef.field("foo");

        assertTrue(fooFieldRef.isResolved());

        Bean bean1 = new Bean("bar");
        Bean bean2 = new Bean();

        fooFieldRef.copy(bean1, bean2);

        assertEquals(bean1.foo, bean2.foo);

    }

    @Test
    public void notAvailableField() throws Exception {

        ClassRef<Bean> classRef = $(Bean.class);
        FieldRef<Bean, Object> bazFieldRef = classRef.field("baz");

        assertFalse(bazFieldRef.isResolved());

        Bean bean1 = new Bean("bar");
        Bean bean2 = new Bean();

        try {
            bazFieldRef.copy(bean1, bean2);
            fail("Should have failed");
        } catch (Exception e) {
            assertNotNull(e);
        }

        assertNull(bean2.foo);

    }

}