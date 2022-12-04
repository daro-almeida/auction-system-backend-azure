package scc.exception;

public class BadCredentialsException extends ServiceException {
    public BadCredentialsException() {
        super("Bad credentials");
    }

    public BadCredentialsException(String message) {
        super("Bad credentials: " + message);
    }
}
