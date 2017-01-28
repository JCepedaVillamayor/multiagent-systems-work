package crawlers;

public class CannotSearchException extends Exception {
    /**
     * Constructor
     *
     * @param message
     */
    public CannotSearchException(String message) {
        super();
        System.out.println(message);
    }
}
