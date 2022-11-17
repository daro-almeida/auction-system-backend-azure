package scc.azure.dao;

public class BidDAO {
    private String _rid;
    private String _ts;
    private String id;
    private String auctionId;
    private String userId;
    private long amount;

    public BidDAO(String auctionId, String userId, long amount) {
        this._rid = null;
        this._ts = null;
        this.id = null;
        this.auctionId = auctionId;
        this.userId = userId;
        this.amount = amount;
    }

    public BidDAO(String id, String auctionId, String userId, long amount) {
        this._rid = null;
        this._ts = null;
        this.id = id;
        this.auctionId = auctionId;
        this.userId = userId;
        this.amount = amount;
    }

    public BidDAO() {
    }

    public String get_rid() {
        return _rid;
    }

    public void set_rid(String _rid) {
        this._rid = _rid;
    }

    public String get_ts() {
        return _ts;
    }

    public void set_ts(String _ts) {
        this._ts = _ts;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "BidDAO [_rid=" + _rid + ", _ts=" + _ts + ", id=" + id + ", auctionId=" + auctionId + ", userId="
                + userId + ", amount=" + amount + "]";
    }

}
