package scc.exception;

public class BidNotFoundException extends ServiceException {
    public BidNotFoundException() {
        super("Bid not found");
    }
}
