package scc.exception;

public class BadRequestException extends ServiceException {

    public BadRequestException() {
        super("");
    }

    public BadRequestException(String message) {
        super(message);
    }
}
