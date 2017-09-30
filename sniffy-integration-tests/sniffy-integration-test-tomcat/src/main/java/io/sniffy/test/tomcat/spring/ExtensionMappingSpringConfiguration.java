package io.sniffy.test.tomcat.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

@Configuration
@RestController
@EnableWebMvc
public class ExtensionMappingSpringConfiguration {

    @PostConstruct
    public void postConstruct() {
        System.out.println("WOW!!!");
    }

    @GetMapping("/base/{pathParam}")
    public String controllerMethod(@PathVariable("pathParam") String pathParam, HttpServletRequest httpServletRequest) {
        Object bmp = httpServletRequest.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        // Context path = "/test"
        // Servlet path = "/base/foo.do"
        // BMP = "/base/{pathParam}.*"

        return pathParam;
    }

}
