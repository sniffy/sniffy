package io.sniffy.tls;

import io.sniffy.reflection.field.StaticFieldRef;
import org.junit.Test;
import sun.security.jca.Providers;

import java.util.Map;

import static io.sniffy.reflection.Unsafe.$;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProvidersListTest {

    @Test
    public void testProvidersFields() {

        Map<String, StaticFieldRef<Object>> staticFields = $(Providers.class).findStaticFields(null, false);

        assertNotNull(staticFields.get("threadLists"));
        assertNotNull(staticFields.get("threadListsUsed"));
        assertNotNull(staticFields.get("providerList"));

        staticFields.remove("threadLists");
        staticFields.remove("threadListsUsed");
        staticFields.remove("providerList");

        staticFields.remove("BACKUP_PROVIDER_CLASSNAME");
        staticFields.remove("jarVerificationProviders");

        assertTrue("Unknown fields in Providers.class: " + staticFields, staticFields.isEmpty());

    }

}
