package io.sniffy.util;

import java.util.AbstractMap;
import java.util.Map;

public class SocketUtil {

    public static Map.Entry<String, Integer> parseSocketAddress(String address) {
        String hostName = null;
        Integer port = null;

        if (null != address) {
            if (-1 != address.indexOf(':')) {
                String[] split = address.split(":");
                hostName = split[0];
                port = Integer.valueOf(split[1]);
            } else {
                hostName = address;
            }
        }

        return new AbstractMap.SimpleEntry<String, Integer>(hostName, port);
    }

}
