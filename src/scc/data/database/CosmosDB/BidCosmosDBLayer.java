package scc.data.database.CosmosDB;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
import scc.data.database.CosmosDB.CosmosDBLayer;
import scc.data.database.BidDAO;

/**
 * Access to Bids database
 */
public class BidCosmosDBLayer extends CosmosDBLayer {

    private static final String BIDS_CONTAINER_NAME = "bids";

    private final CosmosContainer bids;

    /**
     * Default constructor
     */
    public BidCosmosDBLayer(){
        this.bids = db.getContainer(BIDS_CONTAINER_NAME);
    }

    /**
     * Creates an entry for the database with the given bid
     * @param bid Bid that is to be inserted into the database
     * @return Response of the creation of the entry
     */
    public CosmosItemResponse<Object> putBid(BidDAO bid){
        return bids.createItem(bid);
    }

    /**
     * Lists all bids that associated with a given auction
     * @param auctionId Identifier of the auction
     * @return List of all bids to which were made in the given auction
     */
    public CosmosPagedIterable<BidDAO> listBidsByAuction(String auctionId){
        return bids.queryItems("SELECT * FROM bids WHERE bids.auctionId =\"" + auctionId + "\"", new CosmosQueryRequestOptions(),
                BidDAO.class);
    }

}
