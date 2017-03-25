package io.sniffy.servlet;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Issue;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class HtmlInjectorTest {

    @Test
    public void testInjectAtTheEnd() throws Exception {
        assertEquals(
                "<html><head><title>Title</title></head><body>Hello, World!<injected/></body></html>", injectAtTheEnd(
                        "<html><head><title>Title</title></head><body>Hello, World!</body></html>"
                ));
        assertEquals(
                "<html><head><title>Title</title></head><body>Hello, World!<injected/></html>", injectAtTheEnd(
                        "<html><head><title>Title</title></head><body>Hello, World!</html>"
                ));
        assertEquals(
                "<html><head><title>Title</title></head><body>Hello, World!<injected/>", injectAtTheEnd(
                        "<html><head><title>Title</title></head><body>Hello, World!"
                ));
    }

    @Test
    public void testInjectAtTheBeginning() throws Exception {
        assertEquals(
                "<!DOCTYPE html><html><head><injected/><title>Title</title></head><body>Hello, World!</body></html>",
                injectAtTheBeginning(
                        "<!DOCTYPE html><html><head><title>Title</title></head><body>Hello, World!</body></html>"
                ));
        assertEquals(
                "<!DOCTYPE html><html><injected/><title>Title</title></head><body>Hello, World!</body></html>",
                injectAtTheBeginning(
                        "<!DOCTYPE html><html><title>Title</title></head><body>Hello, World!</body></html>"
                ));
        assertEquals(
                "<!DOCTYPE html><injected/><title>Title</title></head><body>Hello, World!</body></html>",
                injectAtTheBeginning(
                        "<!DOCTYPE html><title>Title</title></head><body>Hello, World!</body></html>"
                ));
        assertEquals(
                "<title>Title</title><injected/><script>alert();</script></head><body>Hello, World!</body></html>",
                injectAtTheBeginning(
                        "<title>Title</title><script>alert();</script></head><body>Hello, World!</body></html>"
                ));

        assertEquals(
                "<!DOCTYPE html><html><head><meta http-equiv=\"X-UA-Compatible\" content=\"IE=9; IE=8; IE=7; IE=EDGE\" /><injected/><title>Title</title></head><body>Hello, World!</body></html>",
                injectAtTheBeginning(
                        "<!DOCTYPE html><html><head><meta http-equiv=\"X-UA-Compatible\" content=\"IE=9; IE=8; IE=7; IE=EDGE\" /><title>Title</title></head><body>Hello, World!</body></html>"
                ));
        assertEquals(
                "<!DOCTYPE html><html><head><meta http-equiv=\"X-UA-Compatible\" content=\"IE=9; IE=8; IE=7; IE=EDGE\" ><injected/><title>Title</title></head><body>Hello, World!</body></html>",
                injectAtTheBeginning(
                        "<!DOCTYPE html><html><head><meta http-equiv=\"X-UA-Compatible\" content=\"IE=9; IE=8; IE=7; IE=EDGE\" ><title>Title</title></head><body>Hello, World!</body></html>"
                ));
        assertEquals(
                "<!DOCTYPE html><html><head><meta http-equiv=\"X-UA-Compatible\" content=\"IE=9; IE=8; IE=7; IE=EDGE\" ></meta><injected/><title>Title</title></head><body>Hello, World!</body></html>",
                injectAtTheBeginning(
                        "<!DOCTYPE html><html><head><meta http-equiv=\"X-UA-Compatible\" content=\"IE=9; IE=8; IE=7; IE=EDGE\" ></meta><title>Title</title></head><body>Hello, World!</body></html>"
                ));
        assertEquals(
                "<!DOCTYPE html><html><head><injected/><script> </script><meta http-equiv=\"X-UA-Compatible\" content=\"IE=9; IE=8; IE=7; IE=EDGE\" /><title>Title</title></head><body>Hello, World!</body></html>",
                injectAtTheBeginning(
                        "<!DOCTYPE html><html><head><script> </script><meta http-equiv=\"X-UA-Compatible\" content=\"IE=9; IE=8; IE=7; IE=EDGE\" /><title>Title</title></head><body>Hello, World!</body></html>"
                ));
    }

    @Test
    @Issue("issues/321")
    public void testInjectBeforeBaseTag() throws Exception {
        assertEquals(
                "<!DOCTYPE html><html><head><injected/><base href=\"/\"><meta http-equiv=\"X-UA-Compatible\" content=\"IE=9; IE=8; IE=7; IE=EDGE\" /><title>Title</title></head><body>Hello, World!</body></html>",
                injectAtTheBeginning(
                        "<!DOCTYPE html><html><head><base href=\"/\"><meta http-equiv=\"X-UA-Compatible\" content=\"IE=9; IE=8; IE=7; IE=EDGE\" /><title>Title</title></head><body>Hello, World!</body></html>"
                ));
    }

    private String injectAtTheEnd(String actualContent) throws IOException {
        Buffer buffer = new Buffer();
        buffer.write(actualContent.getBytes());

        HtmlInjector htmlInjector = new HtmlInjector(buffer);
        htmlInjector.injectAtTheEnd("<injected/>");

        return buffer.toString();
    }

    private String injectAtTheBeginning(String actualContent) throws IOException {
        Buffer buffer = new Buffer();
        buffer.write(actualContent.getBytes());

        HtmlInjector htmlInjector = new HtmlInjector(buffer);
        htmlInjector.injectAtTheBeginning("<injected/>");

        return buffer.toString();
    }

}