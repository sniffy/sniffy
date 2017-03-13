package io.sniffy.servlet;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static io.sniffy.servlet.SniffyFilter.HEADER_REQUEST_DETAILS;
import static io.sniffy.servlet.SniffyFilter.REQUEST_URI_PREFIX;
import static io.sniffy.servlet.SniffyFilter.SNIFFY_URI_PREFIX;
import static org.junit.Assert.assertTrue;

@Deprecated
public class SnifferFilterTest extends SniffyFilterTest {

    @Test
    @Deprecated
    public void testFilterSniffyInjected() throws IOException, ServletException, ParserConfigurationException, SAXException {

        answerWithContent("<html><head><title>Title</title></head><body>Hello, World!</body></html>");

        SniffyFilter filter = new SnifferFilter();
        filter.init(getFilterConfig());

        filter.doFilter(requestWithPathAndQueryParameter, httpServletResponse, filterChain);

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = documentBuilder.parse(new ByteArrayInputStream(httpServletResponse.getContentAsString().getBytes()));

        String sniffyJsSrc = doc.getElementsByTagName("script").item(0).getAttributes().getNamedItem("src").getNodeValue();

        assertTrue(sniffyJsSrc + " must be a relative path", sniffyJsSrc.startsWith("../" + SNIFFY_URI_PREFIX));

        String requestDetailsUrl = httpServletResponse.getHeader(HEADER_REQUEST_DETAILS);

        assertTrue(requestDetailsUrl + " must be a relative path", requestDetailsUrl.startsWith("../" + REQUEST_URI_PREFIX));
    }

}
