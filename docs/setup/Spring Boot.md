Using Sniffy with Spring Boot
============

If you are using [Spring Boot](http://projects.spring.io/spring-boot/), simply add `@EnableSniffy` to your application class:

```java
package com.acme;

import io.sniffy.boot.EnableSniffy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration
@EnableSniffy
public class Application {

    public static void main(String[] args) throws ClassNotFoundException {
        SpringApplication.run(Application.class, args);
    }

}
```

You still need to modify the JDBC settings in order to intercept SQL queries:
```yml
spring:
  datasource:
    url: sniffer:jdbc:mysql://127.0.0.1:3306/petstore
    driver-class-name: io.sniffy.MockDriver
```