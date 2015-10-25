package com.github.bedrin.jdbc.sniffer.servlet;

import java.io.*;
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
     * todo support multibyte characters
     * @param content
     * @throws IOException
     */
    public void injectAtTheEnd(String content) throws IOException {

        Reader reader = new InputStreamReader(buffer.reverseInputStream(), characterEncoding);

        char[] htmlClosingTag = new StringBuilder("</html").reverse().toString().toCharArray();
        char[] bodyClosingTag = new StringBuilder("</body").reverse().toString().toCharArray();

        int htmlClosingTagOffset = -1;
        int bodyClosingTagOffset = -1;

        for (int i = reader.read(), htmlBoundaryPos = 0, bodyBoundaryPos = 0, offset = 1;
                (i != -1) && (htmlBoundaryPos < htmlClosingTag.length || bodyBoundaryPos < bodyClosingTag.length);
                i = reader.read(), offset++) {

            if (htmlBoundaryPos < htmlClosingTag.length) {
                if (Character.toLowerCase(i) == htmlClosingTag[htmlBoundaryPos]) {
                    htmlBoundaryPos++;
                } else {
                    htmlBoundaryPos = 0;
                }
            }
            if (bodyBoundaryPos < bodyClosingTag.length) {
                if (Character.toLowerCase(i) == bodyClosingTag[bodyBoundaryPos]) {
                    bodyBoundaryPos++;
                } else {
                    bodyBoundaryPos = 0;
                }
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
            buffer.insertAt(buffer.size() - bodyClosingTagOffset, content.getBytes(characterEncoding));
        } else if (-1 != htmlClosingTagOffset) {
            buffer.insertAt(buffer.size() - htmlClosingTagOffset, content.getBytes(characterEncoding));
        } else {
            buffer.write(content.getBytes(characterEncoding));
        }

    }

}
