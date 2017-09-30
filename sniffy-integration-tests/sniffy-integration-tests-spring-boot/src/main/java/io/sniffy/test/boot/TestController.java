package io.sniffy.test.boot;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

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

    @GetMapping("/ouch/{pathParam}")
    public String ouchService(@PathVariable("pathParam") String pathParam, HttpServletRequest httpServletRequest) {
        throw new OuchException("Ouch! " + pathParam);
    }

}
