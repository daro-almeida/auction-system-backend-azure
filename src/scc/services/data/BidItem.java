package scc.services.data;

import scc.azure.dao.BidDAO;

public class BidItem {
    private final String id;
    private final String userId;
    private final String auctionId;
    private final long amount;

    public BidItem(String id, String userId, String auctionId, long amount) {
        this.id = id;
        this.userId = userId;
        this.auctionId = auctionId;
        this.amount = amount;
    }

    public static BidItem fromBidDAO(BidDAO bidDAO) {
        return new BidItem(bidDAO.getId(), bidDAO.getUserId(), bidDAO.getAuctionId(), bidDAO.getAmount());
    }

    public String getUserId() {
        return userId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public long getAmount() {
        return amount;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "BidItem [id=" + id + ", userId=" + userId + ", auctionId=" + auctionId + ", value=" + amount + "]";
    }

}
