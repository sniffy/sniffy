package io.sniffy.util;

import org.junit.Test;

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

        ReflectionFieldCopier reflectionFieldCopier = new ReflectionFieldCopier(Bean.class, "foo");
        assertTrue(reflectionFieldCopier.isAvailable());

        Bean bean1 = new Bean("bar");
        Bean bean2 = new Bean();

        reflectionFieldCopier.copy(bean1, bean2);

        assertEquals(bean1.foo, bean2.foo);

    }

    @Test
    public void notAvailableField() throws Exception {

        ReflectionFieldCopier reflectionFieldCopier = new ReflectionFieldCopier(Bean.class, "baz");
        assertFalse(reflectionFieldCopier.isAvailable());

        Bean bean1 = new Bean("bar");
        Bean bean2 = new Bean();

        reflectionFieldCopier.copy(bean1, bean2);

        assertEquals(null, bean2.foo);

    }

}