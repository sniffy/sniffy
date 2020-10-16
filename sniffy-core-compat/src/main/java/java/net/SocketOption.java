package java.net;

@Deprecated
public interface SocketOption<T> {

    String name();

    Class<T> type();

}