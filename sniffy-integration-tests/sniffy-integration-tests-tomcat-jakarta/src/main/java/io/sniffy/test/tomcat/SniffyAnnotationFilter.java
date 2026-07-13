package io.sniffy.test.tomcat;

import io.sniffy.servlet.SniffyFilter;
import jakarta.servlet.annotation.WebFilter;

@WebFilter("/*")
public class SniffyAnnotationFilter extends SniffyFilter {
}
