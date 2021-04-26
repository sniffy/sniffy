package io.sniffy.log;

public class OneTimePolyglogImpl extends AbstractPolyglogImpl {

    private boolean logged = false;

    private final Polyglog delegate;

    private final static String DISCLAIMER = " further invocations of this method will not be logged";

    public OneTimePolyglogImpl(Polyglog delegate) {
        this.delegate = delegate;
    }

    @Override
    public void log(PolyglogLevel level, String message) {
        if (logged) return;
        delegate.log(level, message + DISCLAIMER);
        logged = true;
    }

    @Override
    public void error(String message, Throwable e) {
        if (logged) return;
        delegate.error(message + DISCLAIMER, e);
        logged = true;
    }

    @Override
    public void error(Throwable e) {
        if (logged) return;
        delegate.error("Logged exception. Further similar exceptions will not be logged in given statement", e);
        logged = true;
    }

}
