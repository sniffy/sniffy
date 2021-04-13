package io.sniffy.tls;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.Provider;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SniffySSLContextSpiProviderServiceTest {

    @Mock
    private Provider delegateProvider;

    @Mock
    private Provider sniffyProvider;

    @Test
    public void testParameters() throws Exception {

        Provider.Service delegateService = new Provider.Service(
                delegateProvider,
                "DelegateType",
                "DelegateAlgorithm",
                "java.lang.Object",
                Collections.singletonList("DelegateAlias"),
                new HashMap<String, String>() {{
                    put("param1", "value1");
                    put("param2", "value2");
                }}
        );

        when(delegateProvider.getServices()).thenReturn(Collections.singleton(delegateService));

        SniffySSLContextSpiProvider sniffyProvider = new SniffySSLContextSpiProvider(
                delegateProvider,
                "name",
                1.0,
                "info"
        );

        Provider.Service service = sniffyProvider.getService("DelegateType", "DelegateAlgorithm");

        assertNotNull(service);

        assertEquals("DelegateType", service.getType());
        assertEquals("DelegateAlgorithm", service.getAlgorithm());
        assertEquals(sniffyProvider, service.getProvider());
        assertEquals("value1", service.getAttribute("param1"));
        assertEquals("value2", service.getAttribute("param2"));
        assertEquals("java.lang.Object", service.getClassName());
        assertNull(service.getAttribute("param3"));

        Provider.Service serviceAlias = sniffyProvider.getService("DelegateType", "DelegateAlias");

        assertEquals(service, serviceAlias);


    }


}