package scc.resources.data;

import scc.services.data.BidItem;

public class BidDTO {
    public String bidderId;
    public Long value;

    public BidDTO() {
    }

    public BidDTO(String bidderId, Long value) {
        this.bidderId = bidderId;
        this.value = value;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "BidDTO [bidderId=" + bidderId + ", value=" + value + "]";
    }

    public static BidDTO from(BidItem bidItem) {
        return new BidDTO(bidItem.getUserId(), bidItem.getAmount());
    }
}
