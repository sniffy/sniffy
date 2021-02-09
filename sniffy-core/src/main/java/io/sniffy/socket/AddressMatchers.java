package io.sniffy.socket;

import io.sniffy.util.SocketUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;

public class AddressMatchers {

    public static AddressMatcher exactAddressMatcher(String address) {

        Map.Entry<String, Integer> hostAndPort = SocketUtil.parseSocketAddress(address);

        String hostName = hostAndPort.getKey();
        Integer port = hostAndPort.getValue();

        return new ExactAddressMatcher(hostName, port);

    }

    public static AddressMatcher anyAddressMatcher() {
        return new AnyAddressMatcher();
    }

    private static final class ExactAddressMatcher implements AddressMatcher {

        private final String hostName;
        private final Integer port;

        public ExactAddressMatcher(String hostName, Integer port) {
            this.hostName = hostName;
            this.port = port;
        }

        @Override
        public boolean matches(InetSocketAddress inetSocketAddress) {
            InetAddress inetAddress = inetSocketAddress.getAddress();
            return (null == hostName || hostName.equalsIgnoreCase(inetAddress.getHostName()) || hostName.equalsIgnoreCase(inetAddress.getHostAddress())) &&
                    (null == port || port == inetSocketAddress.getPort());
        }

        @Override
        public void describe(StringBuilder appendable) {
            if (null != hostName) {
                appendable.append(hostName);
            }
            if (null != port) {
                appendable.append(":").append(port);
            }
        }
    }

    private static final class AnyAddressMatcher implements AddressMatcher {

        @Override
        public boolean matches(InetSocketAddress inetSocketAddress) {
            return true;
        }

        @Override
        public void describe(StringBuilder appendable) {

        }
    }

}
