package com.github.bedrin.jdbc.sniffer.servlet;

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

        StringBuilder sb = new StringBuilder(new String(buffer.trailingBytes(16 * 1024), characterEncoding));

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
            int offset = sb.delete(0, i).toString().getBytes(characterEncoding).length;
            buffer.insertAt(buffer.size() - offset, content.getBytes(characterEncoding));
        }

    }

}
