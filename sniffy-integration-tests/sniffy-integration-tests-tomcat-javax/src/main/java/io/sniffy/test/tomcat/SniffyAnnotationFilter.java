package io.sniffy.test.tomcat;

import io.sniffy.servlet.SniffyFilter;

import javax.servlet.annotation.WebFilter;

@WebFilter("/*")
public class SniffyAnnotationFilter extends SniffyFilter {
}
