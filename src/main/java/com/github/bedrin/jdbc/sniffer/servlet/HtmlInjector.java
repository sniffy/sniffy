package com.github.bedrin.jdbc.sniffer.servlet;

import java.io.*;
import java.nio.charset.Charset;

class HtmlInjector {

    private final Buffer buffer;
    private final Charset charset;

    public HtmlInjector(Buffer buffer) {
        this(buffer, Charset.defaultCharset());
    }

    public HtmlInjector(Buffer buffer, Charset charset) {
        this.buffer = buffer;
        this.charset = charset;
    }

    /**
     * todo support multibyte characters
     * @param content
     * @throws IOException
     */
    public void injectAtTheEnd(String content) throws IOException {

        Reader reader = new InputStreamReader(buffer.reverseInputStream(), charset);

        char[] htmlClosingTag = new StringBuilder("</html").reverse().toString().toCharArray();
        char[] bodyClosingTag = new StringBuilder("</body").reverse().toString().toCharArray();

        int htmlClosingTagOffset = -1;
        int bodyClosingTagOffset = -1;

        for (int i = reader.read(), htmlBoundaryPos = 0, bodyBoundaryPos = 0, offset = 1;
                (i != -1) && (htmlBoundaryPos < htmlClosingTag.length || bodyBoundaryPos < bodyClosingTag.length);
                i = reader.read(), offset++) {

            if (htmlBoundaryPos < htmlClosingTag.length && Character.toLowerCase(i) == htmlClosingTag[htmlBoundaryPos]) {
                htmlBoundaryPos++;
            }
            if (bodyBoundaryPos < bodyClosingTag.length && Character.toLowerCase(i) == bodyClosingTag[bodyBoundaryPos]) {
                bodyBoundaryPos++;
            }

            if (htmlBoundaryPos == htmlClosingTag.length && -1 == htmlClosingTagOffset) {
                htmlClosingTagOffset = offset;
            }
            if (bodyBoundaryPos == bodyClosingTag.length && -1 == bodyClosingTagOffset) {
                bodyClosingTagOffset = offset;
            }

            if (htmlBoundaryPos == htmlClosingTag.length && bodyBoundaryPos == bodyClosingTag.length) break;

        }

        if (-1 != bodyClosingTagOffset) {
            buffer.insertAt(buffer.size() - bodyClosingTagOffset, content.getBytes(charset));
        } else if (-1 != htmlClosingTagOffset) {
            buffer.insertAt(buffer.size() - htmlClosingTagOffset, content.getBytes(charset));
        } else {
            buffer.write(content.getBytes(charset));
        }

    }

}
