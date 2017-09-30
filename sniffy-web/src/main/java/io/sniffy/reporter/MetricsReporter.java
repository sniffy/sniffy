package io.sniffy.reporter;

import io.sniffy.CurrentThreadSpy;
import io.sniffy.servlet.RequestStats;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface MetricsReporter {

    void init();

    void report(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, String requestId, CurrentThreadSpy spy, RequestStats requestStats);

}
