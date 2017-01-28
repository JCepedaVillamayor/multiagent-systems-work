package crawlers;

public class NotFoundException extends Exception {
    public NotFoundException() {
        super();
        System.out.println("The query couln't be found");
    }

    public NotFoundException(String message) {
        System.out.printf(message);
    }
}
