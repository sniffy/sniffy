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

    public void injectAtTheEnd() throws IOException {

        InputStream inputStream = buffer.reverseInputStream();

        byte[] htmlClosingTag = new StringBuilder("</html").reverse().toString().getBytes(charset);
        byte[] bodyClosingTag = new StringBuilder("</body").reverse().toString().getBytes(charset);

        int htmlClosingTagOffset = -1;
        int bodyClosingTagOffset = -1;

        for (int i = inputStream.read(), htmlBoundaryPos = 0, bodyBoundaryPos = 0, offset = 1;
                (i != -1) && (htmlBoundaryPos < htmlClosingTag.length || bodyBoundaryPos < bodyClosingTag.length);
                i = inputStream.read(), offset++) {

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

        System.out.println(htmlClosingTagOffset);
        System.out.println(bodyClosingTagOffset);


    }

}
