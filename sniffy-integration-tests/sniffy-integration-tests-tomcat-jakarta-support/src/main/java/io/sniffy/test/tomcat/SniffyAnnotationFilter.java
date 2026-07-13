package io.sniffy.test.tomcat;

import io.sniffy.servlet.SniffyFilter;
import jakarta.servlet.annotation.WebFilter;

/** Shared Jakarta filter fixture executed by all Jakarta Tomcat runners. */
@WebFilter("/*")
public class SniffyAnnotationFilter extends SniffyFilter {
}
