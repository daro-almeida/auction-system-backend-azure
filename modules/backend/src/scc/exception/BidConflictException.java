package scc.exception;

public class BidConflictException extends ServiceException {
    public BidConflictException() {
        super("Bid conflict");
    }
}
