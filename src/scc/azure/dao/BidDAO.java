package scc.azure.dao;

public class BidDAO {
    private String _rid;
    private String _ts;
    private String bidId;
    private String auctionId;
    private String userId;
    private long amount;

    public BidDAO(String auctionId, String userId, long amount) {
        this._rid = null;
        this._ts = null;
        this.bidId = null;
        this.auctionId = auctionId;
        this.userId = userId;
        this.amount = amount;
    }

    public BidDAO(String bidId, String auctionId, String userId, long amount) {
        this._rid = null;
        this._ts = null;
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.userId = userId;
        this.amount = amount;
    }

    public BidDAO() {
    }

    public String getBidId() {
        return bidId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getUserId() {
        return userId;
    }

    public long getAmount() {
        return amount;
    }

    @Override
    public String toString() {
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
