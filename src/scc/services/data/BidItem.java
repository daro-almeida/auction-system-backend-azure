package scc.services.data;

public class BidItem {
    private final String id;
    private final String bidderId;
    private final long value;

    public BidItem(String id, String bidderId, long value) {
        this.id = id;
        this.bidderId = bidderId;
        this.value = value;
    }

    public String getBidderId() {
        return bidderId;
    }

    public long getValue() {
        return value;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "BidItem [id=" + id + ", bidderId=" + bidderId + ", value=" + value + "]";
    }

}
