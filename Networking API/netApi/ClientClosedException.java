package netApi;

import java.io.IOException;

/**
 * Signals that an operation on a {@link ClientSideConnection} has been attempted, but that the client has been closed using {@link ClientSideConnection#close()}. This can be resolved by calling {@link ClientSideConnection#reopen()}.
 */
public class ClientClosedException extends IOException {
    /**
     * Creates a new ClientClosedException
     * @param s Description of error
     */
    public ClientClosedException(String s) {
        super(s);
    }
    /**
     * Creates a new ClientClosedException
     */
    public ClientClosedException() {
        super();
    }
}
