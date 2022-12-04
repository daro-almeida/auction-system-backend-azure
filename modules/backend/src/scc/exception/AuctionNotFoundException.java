package scc.exception;

public class AuctionNotFoundException extends ServiceException {
    public AuctionNotFoundException(String auctionId) {
        super("Auction not found: " + auctionId);
    }
}
