package io.sniffy.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class StackTraceExtractor {

    private static ExecutorService executorService = Executors.newSingleThreadExecutor();

    private StackTraceExtractor() {
    }

    public static Future<String> getStatckTraceFuture(Method method) {
        return executorService.submit(new StackTraceGenerator(method));
    }

    public static Future<String> getStatckTraceFuture(String packageName) {
        return executorService.submit(new StackTraceGenerator(packageName));
    }

    private static class StackTraceGenerator implements Callable<String> {

        private final Exception exception = new Exception();
        private final Method method;
        private final String packageName;

        public StackTraceGenerator(Method method) {
            this(method, null);
        }

        public StackTraceGenerator(String packageName) {
            this(null, packageName);
        }

        public StackTraceGenerator(Method method, String packageName) {
            this.method = method;
            this.packageName = packageName;
        }

        @Override
        public String call() throws Exception {
            StackTraceElement[] stackTraceElements = exception.getStackTrace();

            if (null != method) {
                Class<?> baseClass = method.getDeclaringClass();
                // skip all elements until proxied call
                int startIndex = 0;
                StackTraceElement baseMethodTrace = null;
                for (int i = 0; i < stackTraceElements.length; i++) {
                    StackTraceElement traceElement = stackTraceElements[i];
                    String traceElementClassName = traceElement.getClassName();
                    if (traceElementClassName.contains("Proxy")
                            && baseClass.isAssignableFrom(Class.forName(traceElementClassName))) {
                        baseMethodTrace = createTraceElement(method, traceElement);
                        startIndex = i + 1;
                        break;
                    }
                }
                if (startIndex == 0) {
                    // no proxy, return entire collection
                    return printStackTrace(Arrays.asList(stackTraceElements));
                } else {
                    List<StackTraceElement> result = new ArrayList<StackTraceElement>();
                    result.add(baseMethodTrace);
                    result.addAll(Arrays.asList(Arrays.copyOfRange(stackTraceElements, startIndex, stackTraceElements.length - 1)));
                    return printStackTrace(result);
                }
            } else if (null != packageName) {
                int startIndex = 0;
                for (int i = 1; i < stackTraceElements.length; i++) {
                    StackTraceElement traceElement = stackTraceElements[i];
                    String traceElementClassName = traceElement.getClassName();
                    if (!traceElementClassName.startsWith(packageName) &&
                            !traceElementClassName.startsWith("java") &&
                            !traceElementClassName.startsWith("com.sun") &&
                            !traceElementClassName.startsWith("sun") &&
                            !"io.sniffy.socket.SnifferSocketImpl".equals(traceElementClassName) &&
                            !"io.sniffy.socket.SnifferInputStream".equals(traceElementClassName) &&
                            !"io.sniffy.socket.SnifferOutputStream".equals(traceElementClassName) &&
                            !"io.sniffy.util.StackTraceExtractor".equals(traceElementClassName)
                            ) {
                        startIndex = i > 1 ? i - 1 : i;
                        break;
                    }
                    // TODO go back until non java.io. trace
                }
                if (startIndex <= 0) {
                    // no proxy, return entire collection
                    return printStackTrace(Arrays.asList(stackTraceElements));
                } else {
                    return printStackTrace(Arrays.asList(Arrays.copyOfRange(stackTraceElements, startIndex, stackTraceElements.length - 1)));
                }
            } else {
                return "";
            }
        }

    }

    private static String printStackTrace(List<StackTraceElement> stackTraceElements) {
        if (stackTraceElements == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            sb.append(stackTraceElement.toString()).append(lineSeparator);
        }
        if (sb.length() > 0) {
            return sb.substring(0, sb.length() - 1).intern();
        } else {
            return "";
        }
    }

    private static StackTraceElement createTraceElement(Method method, StackTraceElement baseTraceElement) {
        return new StackTraceElement(
                method.getDeclaringClass().getCanonicalName(),
                method.getName(),
                baseTraceElement.getFileName(),
                baseTraceElement.getLineNumber());
    }

}
