package crawlers;

/**
 * Created by jcepeda on 04/11/16.
 */
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
