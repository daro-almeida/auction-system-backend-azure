package scc.srv.resources;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import scc.data.JSON.AuctionJSON;
import scc.data.client.Auction;
import scc.data.client.Bid;
import scc.data.client.Question;
import scc.data.client.Reply;
import scc.srv.mediaStorage.MediaStorage;
import scc.utils.Hash;

import java.util.Base64;
import java.util.Date;

import static scc.data.client.AuctionStatus.*;
import static scc.srv.BuildConstants.*;

@Path("/auction")
public class AuctionsResource {
    private MediaStorage storage;

    public AuctionsResource(MediaStorage storage) {
        this.storage = storage;
    }

    /**
     * Posts a new auction
     * @param auctionJSON JSON which contains the necessary information to create an auction
     * @return Auction's generated identifier
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createAuction(AuctionJSON auctionJSON){
        // Upload the image to blob and get its associated id
        var photo = Base64.getDecoder().decode(auctionJSON.imageBase64());
        var photoId = Hash.of(photo);
        storage.upload(photo);
        //Create the auction with given parameters plus set default for the remaining ones
        var auction = new Auction(auctionJSON.title(),
                auctionJSON.description(),
                photoId,
                auctionJSON.userId(),
                auctionJSON.endTime(),
                auctionJSON.minimumPrice());
        //TODO: Save the generated data to its respective database

        //Return the created auction's id
        return auction.getId();
    }

    /**
     * Updates an existing auction
     * @param auctionId Identifier of the auction
     * @param auctionJSON JSON which contains the info that wants to be changed
     */
    @PATCH
    @Path("/{"+ AUCTION_ID +"}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateAuction(@PathParam(AUCTION_ID) String auctionId,
                              AuctionJSON auctionJSON){
        //TODO: Get the given id and look for auction with same id
        //TODO: If it doesn't exist, return NotFoundException

        //TODO: If the image is not null and its hash is different than current one
        //Upload the image to blob and get its associated id
        var photo = Base64.getDecoder().decode(auctionJSON.imageBase64());
        var photoId = Hash.of(photo);
        storage.upload(photo);
        //TODO: Update the data if the parameters aren't null
    }

    /**
     * Creates a bid on an auction
     * @param auctionId Identifier of the auction
     * @param userId Identifier of the user who performs the bid
     * @param bidAmount Quantity that the user wants to provide for the auction
     * @return Bid's generated identifier
     */
    @POST
    @Path("/{"+ AUCTION_ID +"}/bid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createBid(@PathParam(AUCTION_ID) String auctionId,
                            String userId,
                            long bidAmount){
        //TODO: Check if the auction exists, if not, return NotFoundException
        //TODO: Check if the user exists, if not, return NotFoundException
        //Create the bid with given parameters
        var bid = new Bid(auctionId, userId, bidAmount);
        //TODO: Save the bid into its respective database
        //TODO: Return the created bid's id
        // (don't know if necessary since we only have list operation)
        return bid.getBidId();
    }

    /**
     * Lists all the bids associated with an auction
     * @param auctionId Identifier of the auction
     * @return All the bids that were performed on the auction
     */
    @GET
    @Path("/{"+ AUCTION_ID +"}/bid")
    @Produces(MediaType.TEXT_PLAIN)
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createQuestion(@PathParam(AUCTION_ID) String auctionId,
                                 String userId,
                                 String description){
        //TODO: Check if the auction exists, if not, return NotFoundException
        //TODO: Check if the user exists, if not, return NotFoundException
        //Create the question with given parameters
        var question = new Question(auctionId, userId, description);
        //TODO: Save the question into its respective database
        //TODO: Return the created question's id
        return question.getQuestionId();
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createReply(@PathParam(AUCTION_ID) String auctionId,
                            @PathParam(QUESTION_ID) String questionId,
                            String userId,
                            String description){
        //TODO: Check if the auction exists, if not return NotFoundException
        //TODO: Check if the question exists, if not return NotFoundException
        //TODO: Check if the user exists, if not return NotFoundException
        //TODO: Create the reply with given parameters
        var reply = new Reply(auctionId, questionId, userId, description);
        //TODO: Save the reply into its respective database
        //TODO: Return the created reply's id
        return reply.getReplyId();
    }

    /**
     * Lists all the questions of an auction
     * @param auctionId Identifier of the auction
     * @return All the questions associated with the auction
     */
    @GET
    @Path("/{"+ AUCTION_ID +"}/question")
    @Produces(MediaType.TEXT_PLAIN)
    public String listQuestions(@PathParam(AUCTION_ID) String auctionId){
        //TODO: Check if the auction exists, if not return NotFoundException
        //TODO: Gather all the questions associated to the auctionId
        //TODO: Construct the respective String with collected data then return it
        return null;
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public String listAuctionsAboutToClose(){
        //TODO: Iterate through all auctions and get the ones who about to close (need to define the interval to which we consider "close to end")
        //TODO: Construct the string with collected information and return it
        return null;
    }

    @GET
    @Path("/{" + USER_ID + "}/")
    @Produces(MediaType.TEXT_PLAIN)
    public String listAuctionsOfUser(@PathParam(USER_ID) String userId){
        //TODO: Iterate through all auctions and get the ones whose owner id is the same as the one given
        //TODO: Construct the string with collected information and return it
        return null;
    }
}
