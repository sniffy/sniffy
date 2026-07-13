package io.sniffy.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.util.*;

/**
 * @since 3.1.1
 */
class ServletRegistrationUtil {

    private final static boolean SERVLET_REGISTRATION_API_AVAILABLE;

    static {
        boolean servletRegitrationApiAvailable;
        try {
            Class.forName("javax.servlet.ServletRegistration");
            servletRegitrationApiAvailable = true;
        } catch (ClassNotFoundException e) {
            servletRegitrationApiAvailable = false;
        }
        SERVLET_REGISTRATION_API_AVAILABLE = servletRegitrationApiAvailable;
    }

    public static Set<String> getServletMappings(ServletContext servletContext) {

        if (!SERVLET_REGISTRATION_API_AVAILABLE) return Collections.<String>emptySet();

        Collection<? extends ServletRegistration> servletRegistrations = servletContext.getServletRegistrations().values();
        Set<String> servletMappings = new HashSet<String>();
        for (ServletRegistration servletRegistration : servletRegistrations) {
            servletMappings.addAll(servletRegistration.getMappings());
        }

        return servletMappings;

    }

}
