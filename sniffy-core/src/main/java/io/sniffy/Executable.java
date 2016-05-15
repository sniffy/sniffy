package io.sniffy;

/**
 * Executable interface is similar to {@link Runnable} but it allows throwing {@link Exception}
 * from it's {@link #execute()} method
 * @since 3.1
 */
public interface Executable {

    /**
     * When {@link Sniffy#execute(Executable)}
     * method is called, it will execute the Executable.execute() method, record the SQL queries and return the
     * {@link Spy} object with stats
     * @throws Exception code under test can throw any exception
     */
    void execute() throws Throwable;

}
