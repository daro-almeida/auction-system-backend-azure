package scc.data.client;

/**
 * Represents a bid in an auction
 */
public class Bid {
    private final String bidId;
    private final String auctionId;
    private final String userId;
    private final long amount;

    public Bid(String auctionId,
               String userId,
               long amount){
        this(generateBidId(), auctionId, userId, amount);
    }

    public Bid(String bidId,
               String auctionId,
               String userId,
               long amount){
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.userId = userId;
        this.amount = amount;
    }

    private static String generateBidId(){
        return "0:" + System.currentTimeMillis();
    }

    public String getBidId() {return bidId;}
    public String getAuctionId() {return auctionId;}
    public String getUserId() {return userId;}
    public long getAmount() {return amount;}

    @Override
    public String toString(){
        return "Bid{" +
                "id='" + bidId + '\'' +
                ", auctionId='" + auctionId + '\'' +
                ", userId='" + userId + '\'' +
                ", amount='" + amount + '\'' +
                '}';
    }
}
