package scc.azure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;

import scc.azure.config.CosmosDbConfig;
import scc.azure.dao.BidDAO;
import scc.services.ServiceError;
import scc.utils.Result;

class BidDB {
    private final CosmosContainer container;

    public BidDB(CosmosDatabase db, CosmosDbConfig config) {
        this.container = db.getContainer(config.bidContainer);
    }

    /**
     * Returns the bid saved in the database with given identifier
     * 
     * @param bidId identifier of the bid
     * @return Object that represents a bid
     */
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

    /**
     * Checks if a bid with given identifier exists in the database
     * 
     * @param bidId identifier of the bid
     * @return True if exists in the database, false otherwise
     */
    public boolean bidExists(String bidId) {
        return this.getBid(bidId).isPresent();
    }

    /**
     * Creates an entry in the database that represents a bid
     * 
     * @param bid Object that represents a bid
     * @return 200 with bid's generated identifier
     */
    public Result<BidDAO, ServiceError> createBid(BidDAO bid) {
        if (bid.getId() == null)
            bid.setId(UUID.randomUUID().toString());
        var response = this.container.createItem(bid);
        return Result.ok(response.getItem());
    }

    /**
     * Lists all the bids present in an auction with a given identifier
     * 
     * @param auctionId identifier of the auction
     * @return List of bids made in the auction
     */
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

    private CosmosItemRequestOptions createRequestOptions(String bidId) {
        return new CosmosItemRequestOptions();
    }

}