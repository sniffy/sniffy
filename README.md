Sniffy
============

[![Build Status](https://github.com/sniffy/sniffy/workflows/Build%20and%20deploy/badge.svg)](https://github.com/sniffy/sniffy/actions?query=workflow%3A%22Build+and+deploy%22)
[![Code Coverage](https://codecov.io/gh/sniffy/sniffy/branch/develop/graph/badge.svg)](https://codecov.io/gh/sniffy/sniffy)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.sniffy/sniffy/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/io.sniffy/sniffy)

Sniffy is a Java profiler which shows the results directly in your browser.
It also brings profiling to your unit (or rather component) tests and allows you to disable certain outgoing connections for fault-tolerance testing.
You can also use Sniffy to capture traffic sent or received by your application - just like Wireshark but without admin permission or 3rd-party dependencies.

![RecordedDemo](http://sniffy.io/demo.gif)

Live Demo - [https://demo.sniffy.io/](https://demo.sniffy.io/owners.html?lastName=)

Documentation - [https://www.sniffy.io/docs/latest/](https://www.sniffy.io/docs/latest/)

Servlet integrations
============

The unsuffixed artifacts are the modern Jakarta variants; the `-javax` artifacts are the legacy Javax variants.
The two namespace variants are mutually exclusive.

| Artifact | Verified compatibility |
| --- | --- |
| `sniffy-web` | Java 8 + Servlet 5 + Tomcat 10.0.27; Java 17+ + Tomcat 10.1.57 (Servlet 6.0) or 11.0.24 (Servlet 6.1) |
| `sniffy-spring` | Java 17+ + Spring Framework 7.0.8 + Spring Boot 4.1.0 |
| `sniffy-web-javax` | Java 8 + Javax Servlet + Tomcat 9.0.120 |
| `sniffy-spring-javax` | Java 8 + Spring Framework 5.3.39 + Spring Boot 2.7.18 |

Support
============
Ask questions on stackoverflow with tag [sniffy](https://stackoverflow.com/questions/tagged/sniffy)

Contribute
============
You are most welcome to contribute to Sniffy!

Read the [instructions to build Sniffy](https://github.com/sniffy/sniffy/wiki/Building-Sniffy) and [contribution guidelines](https://github.com/sniffy/sniffy/blob/master/CONTRIBUTING.md)
