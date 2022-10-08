package scc.data.database;

import scc.data.client.Bid;

public class BidDAO {
    private String _rid;
    private String _ts;
    private final String bidId;
    private final String auctionId;
    private final String userId;
    private final long amount;

    public BidDAO(Bid bid){
        this(bid.getBidId(), bid.getAuctionId(), bid.getUserId(), bid.getAmount());
    }

    public BidDAO(String bidId, String auctionId, String userId, long amount){
        super();
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.userId = userId;
        this.amount = amount;
    }

    public String getBidId() {return bidId;}
    public String getAuctionId() {return auctionId;}
    public String getUserId() {return userId;}
    public long getAmount() {return amount;}

    public Bid toBid(){
        return new Bid(bidId, auctionId, userId, amount);
    }

    @Override
    public String toString(){
        return "BidDAO:{" +
                "_rid='" + _rid + '\'' +
                ", _ts='" + _ts + '\'' +
                ", id='" + bidId + '\'' +
                ", auctionId='" + auctionId + '\'' +
                ", userId='" + userId + '\'' +
                ", amount='" + amount + '\'' +
                '}';

    }
}
