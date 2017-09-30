package io.sniffy.test.tomcat.spring;

import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

@WebServlet(
        value = "/servlet/*",
        initParams = {
                @WebInitParam(
                        name = "contextClass",
                        value = "org.springframework.web.context.support.AnnotationConfigWebApplicationContext"
                ),
                @WebInitParam(
                        name = "contextConfigLocation",
                        value = "io.sniffy.test.tomcat.spring.PathMappingSpringConfiguration"
                )
        },
        loadOnStartup = 1
)
public class DispatcherServletWithPathMapping extends DispatcherServlet {



}
