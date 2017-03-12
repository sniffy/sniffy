package io.sniffy.test.boot;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TestController {

    public static class OuchException extends RuntimeException {
        public OuchException(String message) {
            super(message);
        }
    }

    @GetMapping("/restservice")
    @ResponseBody
    public String restService() {
        return "\"Hello, world!\"";
    }

    @GetMapping("/ouch")
    public String ouchService() {
        throw new OuchException("Ouch!");
    }

}
