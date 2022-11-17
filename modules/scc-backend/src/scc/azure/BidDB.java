package scc.azure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;

import scc.azure.config.CosmosDbConfig;
import scc.azure.dao.BidDAO;
import scc.azure.dao.QuestionDAO;
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
    public Result<List<BidDAO>, ServiceError> listBids(String auctionId) {
        var options = this.createQueryOptions(auctionId);
        return Result.ok(this.container
                .queryItems(
                        "SELECT * FROM bids WHERE bids.auctionId=\"" + auctionId + "\"",
                        options,
                        BidDAO.class)
                .stream().collect(Collectors.toList()));
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

    /**
     * Delete all the bids related to the user with given identifier
     * @param userId identifier of the user
     * @return 204
     */
    public Result<Void, ServiceError> deleteBidsFromUser(String userId) {
        for (BidDAO bid : userBids(userId)) {
            var result = deleteBid(bid.getId());
            if (!result.isOk())
                System.out.printf("deleteUserQuestions: question %s not found\n", bid.getId());
        }
        return Result.ok();
    }

    /**
     * Delete a bid with given identifier from the database
     * @param bidId identifier of the bid
     * @return 200 with deleted bid's identifier, 404 if it doesn't exist
     */
    private Result<String, ServiceError> deleteBid(String bidId) {
        var options = new CosmosItemRequestOptions();
        var partitionKey = this.createPartitionKey(bidId);
        try {
            this.container.deleteItem(bidId, partitionKey, options);
            return Result.ok(bidId);
        } catch (CosmosException e) {
            if (e.getStatusCode() == 404)
                return Result.err(ServiceError.BID_NOT_FOUND);
            throw e;
        }
    }

    /**
     * Gets the list of bids made by the user with given identifier
     * @param userId identifier of the user
     * @return List of bids from the user
     */
    private List<BidDAO> userBids(String userId) {
        return this.container
                .queryItems(
                        "SELECT * FROM bids WHERE bids.userId=\"" + userId + "\"",
                        new CosmosQueryRequestOptions(),
                        BidDAO.class)
                .stream().toList();
    }
}