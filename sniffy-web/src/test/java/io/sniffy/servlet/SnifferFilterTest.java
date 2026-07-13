package io.sniffy.servlet;

import org.junit.Test;
import org.xml.sax.SAXException;

import javax.script.ScriptException;
import javax.servlet.ServletException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static io.sniffy.servlet.SniffyFilter.*;
import static org.junit.Assert.assertTrue;

@Deprecated
public class SnifferFilterTest extends SniffyFilterTest {

    @Test
    @Deprecated
    public void testFilterSniffyInjected() throws IOException, ServletException, ParserConfigurationException, SAXException, ScriptException {

        answerWithContent("<html><head><title>Title</title></head><body>Hello, World!</body></html>");

        SniffyFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

        String sniffyJsSrc = extractSniffyJsSrc(httpServletResponse.getContentAsString());

        assertTrue(sniffyJsSrc + " must be a relative path", sniffyJsSrc.startsWith("../" + SNIFFY_RESOURCE_URI_PREFIX));

        String requestDetailsUrl = httpServletResponse.getHeader(HEADER_REQUEST_DETAILS);

        assertTrue(requestDetailsUrl + " must be a relative path", requestDetailsUrl.startsWith("../" + REQUEST_URI_PREFIX));
    }

}
