package com.github.bedrin.jdbc.sniffer;

public class ThreadLocalSniffer extends ThreadLocal<Integer> {

    @Override
    protected Integer initialValue() {
        return 0;
    }
}
