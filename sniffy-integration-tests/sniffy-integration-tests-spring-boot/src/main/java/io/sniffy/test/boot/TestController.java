package io.sniffy.test.boot;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/restservice")
    public String restService() {
        return "\"Hello, world!\"";
    }

}
