package io.sniffy.servlet.internal;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Namespace-neutral HTML response injector shared by the Servlet integrations.
 */
public class HtmlInjector {

    private final Buffer buffer;
    private final String characterEncoding;

    public HtmlInjector(Buffer buffer) {
        this(buffer, Charset.defaultCharset().name());
    }

    public HtmlInjector(Buffer buffer, String characterEncoding) {
        this.buffer = buffer;
        this.characterEncoding = characterEncoding;
    }

    public void injectAtTheEnd(String content) throws IOException {
        String str = new String(buffer.trailingBytes(16 * 1024), characterEncoding).toLowerCase();
        StringBuilder sb = new StringBuilder(str);

        int htmlLIOf = sb.lastIndexOf("</html");
        int bodyLIOf = sb.lastIndexOf("</body");
        int position;

        if (-1 != bodyLIOf && (-1 == htmlLIOf || bodyLIOf < htmlLIOf)) {
            position = bodyLIOf;
        } else if (-1 != htmlLIOf) {
            position = htmlLIOf;
        } else {
            position = -1;
        }

        if (position == -1) {
            buffer.write(content.getBytes(characterEncoding));
        } else {
            int offset = str.substring(position).getBytes(characterEncoding).length;
            buffer.insertAt(buffer.size() - offset, content.getBytes(characterEncoding));
        }
    }

    public void injectAtTheBeginning(String content) throws IOException {
        String str = new String(buffer.leadingBytes(16 * 1024), characterEncoding).toLowerCase();
        StringBuilder sb = new StringBuilder(str);

        int afterHtml = indexAfterTag(sb, "<html");
        int afterHead = indexAfterTag(sb, "<head");
        int afterDocType = indexAfterTag(sb, "<!doctype");
        int beforeScript = sb.indexOf("<script");
        int beforeBase = sb.indexOf("<base");
        int beforeScriptOrBase = beforeScript >= 0 && beforeBase >= 0 ? Math.min(beforeScript, beforeBase) :
                beforeScript >= 0 ? beforeScript : beforeBase;

        String contentBeforeScript = -1 == beforeScriptOrBase ? sb.toString() :
                sb.substring(0, beforeScriptOrBase);
        int lastMetaOpeningTag = contentBeforeScript.lastIndexOf("<meta");
        int lastMetaClosingTag = contentBeforeScript.lastIndexOf("</meta");
        int afterLastMeta = -1;
        if (lastMetaOpeningTag != -1 || lastMetaClosingTag != -1) {
            afterLastMeta = contentBeforeScript.indexOf(">", Math.max(lastMetaOpeningTag, lastMetaClosingTag)) + 1;
            if (0 == afterLastMeta) {
                afterLastMeta = -1;
            }
        }

        int position;
        if (-1 != afterLastMeta) {
            position = afterLastMeta;
        } else if (-1 != afterHead && (-1 == beforeScriptOrBase || afterHead <= beforeScriptOrBase)) {
            position = afterHead;
        } else if (-1 != afterHtml && (-1 == beforeScriptOrBase || afterHtml <= beforeScriptOrBase)) {
            position = afterHtml;
        } else if (-1 != afterDocType && (-1 == beforeScriptOrBase || afterDocType < beforeScriptOrBase)) {
            position = afterDocType;
        } else if (-1 != beforeScriptOrBase) {
            position = beforeScriptOrBase;
        } else {
            position = 0;
        }

        int offset = str.substring(0, position).getBytes(characterEncoding).length;
        buffer.insertAt(offset, content.getBytes(characterEncoding));
    }

    private static int indexAfterTag(StringBuilder content, String tag) {
        int index = content.indexOf(tag);
        if (index != -1) {
            index = content.indexOf(">", index) + 1;
            if (0 == index) {
                index = -1;
            }
        }
        return index;
    }
}
