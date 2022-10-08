package scc.srv;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import static scc.srv.BuildConstants.*;

@Path("/auction")
public class AuctionsResource {

    public AuctionsResource() {

    }

    /**
     * Posts a new auction
     * @param title Title of the auction
     * @param description Description of the auction
     * @param image Image associated with the auction
     * @param ownerId Identifier of the user who creates the auction
     * @param endTime Time limit for when the bids can be performed until then
     * @param minimumPrice Minimum amount for the bids
     * @return Auction's generated identifier
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public String createAuction(String title,
                                String description,
                                byte[] image,
                                String ownerId,
                                /*Date endTime,*/
                                long minimumPrice){
        //TODO: Upload the image to blob and get its associated id
        //TODO: Create the auction with given parameters plus set default for the remaining ones
        //TODO: Save the generated data to its respective database
        //TODO: Return the created auction's id
        return null;
    }

    /**
     * Updates an existing auction
     * @param auctionId Identifier of the auction
     * @param title Possible new title of the auction
     * @param description Possible new description of the auction
     * @param image Possible new image of the auction
     * @param endTime Possible new time limit of the auction
     * @param minimumPrice Possible new minimum price of the auction
     */
    @PUT
    @Path("/{"+ AUCTION_ID +"}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public void updateAuction(@PathParam(AUCTION_ID) String auctionId,
                              String title,
                              String description,
                              byte[] image,
                              /*Date endTime,*/
                              long minimumPrice){
        //TODO: Get the given id and look for auction with same id
        //TODO: If it doesn't exist, return NotFoundException
        //TODO: If the image is not null and its hash is different than current one
        //TODO: Upload the image to blob and get its associated id
        //TODO: Update the data if the parameters aren't null
    }

    /**
     * Creates a bid on an auction
     * @param auctionId Identifier of the auction
     * @param bidderId Identifier of the user who performs the bid
     * @param bidAmount Quantity that the user wants to provide for the auction
     * @return Bid's generated identifier
     */
    @POST
    @Path("/{"+ AUCTION_ID +"}/bid")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public String createBid(@PathParam(AUCTION_ID) String auctionId,
                            String bidderId,
                            long bidAmount){
        //TODO: Check if the auction exists, if not, return NotFoundException
        //TODO: Check if the user exists, if not, return NotFoundException
        //TODO: Create the bid with given parameters
        //TODO: Save the bid into its respective database
        //TODO: Return the created bid's id
        // (don't know if necessary since we only have list operation)
        return null;
    }

    /**
     * Lists all the bids associated with an auction
     * @param auctionId Identifier of the auction
     * @return All the bids that were performed on the auction
     */
    @GET
    @Path("/{"+ AUCTION_ID +"}/bid")
    @Produces(MediaType.APPLICATION_JSON)
    public String listBids(@PathParam(AUCTION_ID) String auctionId){
        //TODO: Check if the auction exists, if not, return NotFoundException
        //TODO: Gather all the bids associated with given auction
        //TODO: Construct the respective String with the collected data and return it
        return null;
    }

    /**
     * Creates a question on an auction
     * @param auctionId Identifier of the auction
     * @param userId Identifier of the user who performs the question
     * @param description Text of the question
     * @return Question's generated identifier
     */
    @POST
    @Path("/{"+ AUCTION_ID +"}/question")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public String createQuestion(@PathParam(AUCTION_ID) String auctionId,
                                 String userId,
                                 String description){
        //TODO: Check if the auction exists, if not, return NotFoundException
        //TODO: Check if the user exists, if not, return NotFoundException
        //TODO: Create the question with given parameters
        //TODO: Save the question into its respective database
        //TODO: Return the created question's id
        return null;
    }

    /**
     * Creates a reply on a question done in an auction
     * @param auctionId Identifier of the auction
     * @param questionId Identifier of the question
     * @param userId Identifier of the user who performs the reply
     * @param description Text of the reply
     * @return Reply's generated id
     */
    @POST
    @Path("/{"+ AUCTION_ID +"}/question/{"+ QUESTION_ID+"}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public String createReply(@PathParam(AUCTION_ID) String auctionId,
                            @PathParam(QUESTION_ID) String questionId,
                            String userId,
                            String description){
        //TODO: Check if the auction exists, if not return NotFoundException
        //TODO: Check if the question exists, if not return NotFoundException
        //TODO: Check if the user exists, if not return NotFoundException
        //TODO: Create the reply with given parameters
        //TODO: Save the reply into its respective database
        //TODO: Return the created reply's id
        return null;
    }

    /**
     * Lists all the questions of an auction
     * @param auctionId Identifier of the auction
     * @return All the questions associated with the auction
     */
    @GET
    @Path("/{"+ AUCTION_ID +"}/question")
    @Produces(MediaType.APPLICATION_JSON)
    public String listQuestions(@PathParam(AUCTION_ID) String auctionId){
        //TODO: Check if the auction exists, if not return NotFoundException
        //TODO: Gather all the questions associated to the auctionId
        //TODO: Construct the respective String with collected data then return it
        return null;
    }
}
