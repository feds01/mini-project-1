package common;

/**
 * Exception class that represents when a connection unexpectedly drops
 * for any networking reason.
 *
 * @author 200008575
 * */
public class DisconnectedException extends Exception {
    public DisconnectedException(String message) {
        super(message);
    }
}