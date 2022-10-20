package scc.azure;

import java.util.Optional;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosPatchOperations;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;

import scc.azure.config.CosmosDbConfig;
import scc.azure.dao.AuctionDAO;
import scc.services.AuctionService;
import scc.services.UserService;
import scc.utils.Result;

class AuctionDB {
    private final CosmosContainer container;

    public AuctionDB(CosmosDatabase db, CosmosDbConfig config) {
        this.container = db.getContainer(config.auctionContainer);
    }

    /**
     * Gets the auction saved in the database with given identifier
     * @param auctionId identifier of the auction
     * @return Object that represents the auction
     */
    public Optional<AuctionDAO> getAuction(String auctionId) {
        var options = this.createQueryOptions(auctionId);
        return this.container
                .queryItems(
                        "SELECT * FROM auctions WHERE auctions.id=\"" + auctionId + "\"",
                        options,
                        AuctionDAO.class)
                .stream()
                .findFirst();
    }

    /**
     * Checks if the database contains an auction with given identifier
     * @param auctionId identifier of the auction
     * @return true if exists, false otherwise
     */
    public boolean auctionExists(String auctionId) {
        return this.getAuction(auctionId).isPresent();
    }

    /**
     * Creates an entry in the database that represents an auction
     * @param auction Object that represents an auction
     * @return 200 with new auction's generated identifier
     */
    public Result<AuctionDAO, AuctionService.Error> createAuction(AuctionDAO auction) {
        assert auction.getId() == null; // ID should be auto generated
        var response = this.container.createItem(auction);
        return Result.ok(response.getItem());
    }

    /**
     * Deletes an auction which just changes the status to deleted
     * @param auctionId identifier of the auction
     * @return 204 if successful, 404 otherwise
     */
    public Result<Void, AuctionService.Error> deleteAuction(String auctionId) {
        var options = this.createRequestOptions(auctionId);
        var partitionKey = this.createPartitionKey(auctionId);
        try {
            var deleteOps = CosmosPatchOperations.create();
            deleteOps.set("/status", AuctionDAO.Status.DELETED);
            var response = updateAuction(auctionId, deleteOps);
            return Result.ok(response.value());
        } catch (CosmosException e) {
            if (e.getStatusCode() == 404)
                return Result.err(AuctionService.Error.USER_NOT_FOUND);
            throw e;
        }
    }

    /**
     * Updates the values in the database for an auction with given identifier
     * @param auctionId identifier of the auction
     * @param ops operations to be executed on the entry
     * @return 204 if successful, 404 otherwise
     */
    public Result<Void, AuctionService.Error> updateAuction(String auctionId, CosmosPatchOperations ops) {
        var partitionKey = this.createPartitionKey(auctionId);
        try {
            this.container.patchItem(auctionId, partitionKey, ops, AuctionDAO.class);
            return Result.ok();
        } catch (CosmosException e) {
            return Result.err(AuctionService.Error.AUCTION_NOT_FOUND);
        }
    }

    private PartitionKey createPartitionKey(String auctionId) {
        return new PartitionKey(auctionId);
    }

    private CosmosItemRequestOptions createRequestOptions(String auctionId) {
        var options = new CosmosItemRequestOptions();
        return options;
    }

    private CosmosQueryRequestOptions createQueryOptions(String auctionId) {
        return new CosmosQueryRequestOptions().setPartitionKey(this.createPartitionKey(auctionId));
    }
}