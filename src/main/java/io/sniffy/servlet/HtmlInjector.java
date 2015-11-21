package io.sniffy.servlet;

import java.io.IOException;
import java.nio.charset.Charset;

class HtmlInjector {

    private final Buffer buffer;
    private final String characterEncoding;

    public HtmlInjector(Buffer buffer) {
        this(buffer, Charset.defaultCharset().name());
    }

    public HtmlInjector(Buffer buffer, String characterEncoding) {
        this.buffer = buffer;
        this.characterEncoding = characterEncoding;
    }

    /**
     * @param content to be inserted
     * @throws IOException
     */
    public void injectAtTheEnd(String content) throws IOException {

        String str = new String(buffer.trailingBytes(16 * 1024), characterEncoding).toLowerCase();
        StringBuilder sb = new StringBuilder(str);

        int htmlLIOf = sb.lastIndexOf("</html");
        int bodyLIOf = sb.lastIndexOf("</body");

        int i;

        if (-1 != bodyLIOf && (-1 == htmlLIOf || bodyLIOf < htmlLIOf)) {
            i = bodyLIOf;
        } else if (-1 != htmlLIOf) {
            i = htmlLIOf;
        } else {
            i = -1;
        }

        if (i == -1) {
            buffer.write(content.getBytes(characterEncoding));
        } else {
            int offset = str.substring(i).getBytes(characterEncoding).length;
            buffer.insertAt(buffer.size() - offset, content.getBytes(characterEncoding));
        }

    }

    /**
     * @param content to be inserted
     * @throws IOException
     */
    public void injectAtTheBeginning(String content) throws IOException {

        String str = new String(buffer.leadingBytes(16 * 1024), characterEncoding).toLowerCase();
        StringBuilder sb = new StringBuilder(str);

        int htmlIOf = sb.indexOf("<html");
        if (htmlIOf != -1) {
            htmlIOf = sb.indexOf(">", sb.indexOf("<html")) + 1;
        }

        int headIOf = sb.indexOf("<head");
        if (headIOf != -1) {
            headIOf = sb.indexOf(">", sb.indexOf("<head")) + 1;
        }

        int doctypeIOf = sb.indexOf("<!doctype");
        if (doctypeIOf != -1) {
            doctypeIOf = sb.indexOf(">", sb.indexOf("<!doctype")) + 1;
        }

        int scriptIOf = sb.indexOf("<script");

        int i;

        if (-1 != headIOf && (-1 == scriptIOf || headIOf < scriptIOf)) {
            i = headIOf;
        } else if (-1 != htmlIOf && (-1 == scriptIOf || htmlIOf < scriptIOf)) {
            i = htmlIOf;
        } else if (-1 != doctypeIOf && (-1 == scriptIOf || doctypeIOf < scriptIOf)) {
            i = doctypeIOf;
        } else if (-1 != scriptIOf) {
            i = scriptIOf;
        } else {
            i = 0;
        }

        int offset = str.substring(0, i).getBytes(characterEncoding).length;
        buffer.insertAt(offset, content.getBytes(characterEncoding));

    }

}
