package netApi;
/**
 * Thrown when a client tries to connect to a mismatched server
 */
public class ServerTypeMismatchException extends Exception{
    /**
     * Creates a new ServerTypeMismatchException
     * @param s Description of error
     */
    public ServerTypeMismatchException(String s) {
        super(s);
    }
    /**
     * Creates a new ServerTypeMismatchException
     */
    public ServerTypeMismatchException() {
        super();
    }
}
