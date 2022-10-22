package scc.azure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import scc.utils.Result;

class AuctionDB {
    private final CosmosContainer container;

    public AuctionDB(CosmosDatabase db, CosmosDbConfig config) {
        this.container = db.getContainer(config.auctionContainer);
    }

    /**
     * Gets the auction saved in the database with given identifier
     * 
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
     * 
     * @param auctionId identifier of the auction
     * @return true if exists, false otherwise
     */
    public boolean auctionExists(String auctionId) {
        return this.getAuction(auctionId).isPresent();
    }

    /**
     * Creates an entry in the database that represents an auction
     * 
     * @param auction Object that represents an auction
     * @return 200 with new auction's generated identifier
     */
    public Result<AuctionDAO, AuctionService.Error> createAuction(AuctionDAO auction) {
        if (auction.getId() == null)
            auction.setId(UUID.randomUUID().toString());
        var response = this.container.createItem(auction);
        return Result.ok(response.getItem());
    }

    /**
     * Deletes an auction which just changes the status to deleted
     * 
     * @param auctionId identifier of the auction
     * @return 204 if successful, 404 otherwise
     */
    public Result<Void, AuctionService.Error> deleteAuction(String auctionId) {
        try {
            var deleteOps = CosmosPatchOperations.create();
            deleteOps.set("/status", AuctionDAO.Status.DELETED);
            var response = updateAuction(auctionId, deleteOps);
            return Result.ok(response.value());
        } catch (CosmosException e) {
            if (e.getStatusCode() == 404)
                return Result.err(AuctionService.Error.AUCTION_NOT_FOUND);
            throw e;
        }
    }

    public Result<List<AuctionDAO>, AuctionService.Error> listAuctionsOfUser(String userId) {
        var options = this.createQueryOptions();
        var auctions = this.container
                .queryItems(
                        "SELECT * FROM auctions WHERE auctions.userId=\"" + userId + "\"",
                        options,
                        AuctionDAO.class)
                .stream().toList();
        return Result.ok(auctions);
    }

    /**
     * Updates the values in the database for an auction with given identifier
     * 
     * @param auctionId identifier of the auction
     * @param ops       operations to be executed on the entry
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

    /**
     * Set auctions of user with the given userId to status DELETED
     * 
     * @param userId identifier of the user
     * @return 204
     */
    public Result<Void, AuctionService.Error> deleteUserAuctions(String userId) {
        for (AuctionDAO auction : userAuctions(userId)) {
            var result = deleteAuction(auction.getId());
            if (!result.isOk())
                System.out.printf("deleteUserAuctions: auction %s not found\n", auction.getId());
        }
        return Result.ok();
    }

    private List<AuctionDAO> userAuctions(String userId) {
        return this.container
                .queryItems(
                        "SELECT * FROM auctions WHERE auctions.userId=\"" + userId + "\"",
                        new CosmosQueryRequestOptions(),
                        AuctionDAO.class)
                .stream().toList();
    }

    private PartitionKey createPartitionKey(String auctionId) {
        return new PartitionKey(auctionId);
    }

    private CosmosItemRequestOptions createRequestOptions(String auctionId) {
        return new CosmosItemRequestOptions();
    }

    private CosmosQueryRequestOptions createQueryOptions() {
        return new CosmosQueryRequestOptions();

    }

    private CosmosQueryRequestOptions createQueryOptions(String auctionId) {
        return createQueryOptions().setPartitionKey(this.createPartitionKey(auctionId));
    }
}