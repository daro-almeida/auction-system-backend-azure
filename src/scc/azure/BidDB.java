package scc.azure;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;

import scc.azure.config.CosmosDbConfig;
import scc.azure.dao.BidDAO;
import scc.services.AuctionService;
import scc.utils.Result;

class BidDB {
    private final CosmosContainer container;

    public BidDB(CosmosDatabase db, CosmosDbConfig config) {
        this.container = db.getContainer(config.bidContainer);
    }

    public Optional<BidDAO> getBid(String bidId) {
        var options = this.createQueryOptions(bidId);
        return this.container
                .queryItems(
                        "SELECT * FROM bids WHERE bids.id=\"" + bidId + "\"",
                        options,
                        BidDAO.class)
                .stream()
                .findFirst();
    }

    public boolean bidExists(String bidId) {
        return this.getBid(bidId).isPresent();
    }

    public Result<BidDAO, AuctionService.Error> createBid(BidDAO bid) {
        assert bid.getBidId() == null; // Auto-generated
        var response = this.container.createItem(bid);
        return Result.ok(response.getItem());
    }

    public List<BidDAO> listBids(String auctionId) {
        var options = this.createQueryOptions(auctionId);
        return this.container
                .queryItems(
                        "SELECT * FROM bids WHERE bids.auctionId=\"" + auctionId + "\"",
                        options,
                        BidDAO.class)
                .stream().collect(Collectors.toList());
    }

    private PartitionKey createPartitionKey(String bidId) {
        return new PartitionKey(bidId);
    }

    private CosmosQueryRequestOptions createQueryOptions(String bidId) {
        var options = new CosmosQueryRequestOptions();
        options.setPartitionKey(this.createPartitionKey(bidId));
        return options;
    }

}