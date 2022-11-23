package io.sniffy.util;

import io.sniffy.reflection.clazz.ClassRef;
import io.sniffy.reflection.field.UnresolvedNonStaticFieldRef;
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
        UnresolvedNonStaticFieldRef<Bean, Object> fooFieldRef = classRef.getNonStaticField("foo");

        assertTrue(fooFieldRef.isResolved());

        Bean bean1 = new Bean("bar");
        Bean bean2 = new Bean();

        fooFieldRef.copy(bean1, bean2);

        assertEquals(bean1.foo, bean2.foo);

    }

    @Test
    public void notAvailableField() throws Exception {

        ClassRef<Bean> classRef = $(Bean.class);
        UnresolvedNonStaticFieldRef<Bean, Object> bazFieldRef = classRef.getNonStaticField("baz");

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