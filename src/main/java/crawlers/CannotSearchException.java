package crawlers;

/**
 * Created by neverhope on 04/11/16.
 */
public class CannotSearchException extends Exception{
    public CannotSearchException(String message){
        super();
        System.out.println(message);
    }
}
