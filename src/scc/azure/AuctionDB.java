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
import scc.utils.Result;

class AuctionDB {
    private final CosmosContainer container;

    public AuctionDB(CosmosDatabase db, CosmosDbConfig config) {
        this.container = db.getContainer(config.auctionContainer);
    }

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

    public boolean auctionExists(String auctionId) {
        return this.getAuction(auctionId).isPresent();
    }

    public Result<AuctionDAO, AuctionService.Error> createAuction(AuctionDAO auction) {
        assert auction.getId() == null; // ID should be auto generated
        var response = this.container.createItem(auction);
        return Result.ok(response.getItem());
    }

    public Result<Void, AuctionService.Error> deleteAuction(String auctionId) {
        var options = this.createRequestOptions(auctionId);
        var partitionKey = this.createPartitionKey(auctionId);
        var response = this.container.deleteItem(auctionId, partitionKey, options);
        // TODO: Is this error checking correct?
        if (response.getStatusCode() == 204) {
            return Result.ok();
        } else {
            return Result.error(AuctionService.Error.AUCTION_NOT_FOUND);
        }
    }

    public Result<Void, AuctionService.Error> updateAuction(String auctionId, CosmosPatchOperations ops) {
        var partitionKey = this.createPartitionKey(auctionId);
        try {
            this.container.patchItem(auctionId, partitionKey, ops, AuctionDAO.class);
            return Result.ok(null);
        } catch (CosmosException e) {
            return Result.error(AuctionService.Error.AUCTION_NOT_FOUND);
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