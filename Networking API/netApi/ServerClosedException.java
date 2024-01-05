package netApi;

import java.io.IOException;

/**
 * Signals that an operation on a {@link Server} has been attempted, but that the server has been closed using {@link Server#close()}. This can be resolved by calling {@link Server#open()}.
 */
public class ServerClosedException extends IOException {
    /**
     * Creates a new ServerClosedException
     * @param s Description of error
     */
    public ServerClosedException(String s) {
        super(s);
    }
    /**
     * Creates a new ServerClosedException
     */
    public ServerClosedException() {
        super();
    }
}
