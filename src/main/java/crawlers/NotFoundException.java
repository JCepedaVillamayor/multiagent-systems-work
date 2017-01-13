package crawlers;

/**
 * Created by jcepeda on 29/12/16.
 */
public class NotFoundException extends Exception {
    public NotFoundException() {
        super();
        System.out.println("The query couln't be found");
    }

    public NotFoundException(String message) {
        System.out.printf(message);
    }
}
