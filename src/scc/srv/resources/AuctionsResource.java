package scc.srv.resources;

import com.azure.cosmos.models.CosmosPatchOperations;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import scc.data.JSON.AuctionJSON;
import scc.data.client.Auction;
import scc.data.client.Bid;
import scc.data.client.Question;
import scc.data.client.Reply;
import scc.data.database.AuctionDAO;
import scc.data.database.BidDAO;
import scc.data.database.CosmosDB.*;
import scc.data.database.QuestionDAO;
import scc.data.database.ReplyDAO;
import scc.srv.mediaStorage.MediaStorage;
import scc.utils.Hash;

import java.util.Base64;

import static scc.srv.BuildConstants.*;

@Path("/auction")
public class AuctionsResource {
    private final MediaStorage storage;
    private final AuctionCosmosDBLayer dbAuctions;
    private final UserCosmosDBLayer dbUsers;
    private final BidCosmosDBLayer dbBids;
    private final QuestionCosmosDBLayer dbQuestions;
    private final ReplyCosmosDBLayer dbReplies;

    public AuctionsResource(MediaStorage storage) {
        this.storage = storage;
        this.dbAuctions = new AuctionCosmosDBLayer();
        this.dbUsers = new UserCosmosDBLayer();
        this.dbBids = new BidCosmosDBLayer();
        this.dbQuestions = new QuestionCosmosDBLayer();
        this.dbReplies = new ReplyCosmosDBLayer();
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
        //Save the generated data to its respective database
        dbAuctions.putAuction(new AuctionDAO(auction));

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
        //Update the data if the parameters aren't null
        var operations = CosmosPatchOperations.create();
        if(auctionJSON.title() != null) operations.replace("/title", auctionJSON.title());
        if(auctionJSON.description() != null) operations.replace("/description", auctionJSON.description());
        if(auctionJSON.imageBase64() != null){
            //Upload the image to blob and get its associated id
            var photo = Base64.getDecoder().decode(auctionJSON.imageBase64());
            var photoId = Hash.of(photo);
            storage.upload(photo);
            operations.replace("/photoId", photoId);
        }
        if(auctionJSON.endTime() != null) operations.replace("/endTime", auctionJSON.endTime());
        if(auctionJSON.minimumPrice() >= 0) operations.replace("/minimumPrice", auctionJSON.minimumPrice());

        //Get the given id and look for auction with same id
        //If it doesn't exist, return NotFoundException
        var response = dbAuctions.updateAuction(auctionId, operations);
        if(response.getStatusCode() == 404) throw new NotFoundException();
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
        //Check if the auction exists, if not, return NotFoundException
        var responseAuction = dbAuctions.getAuctionById(auctionId);
        if (responseAuction.isEmpty()) throw new NotFoundException();

        //Check if the user exists, if not, return NotFoundException
        var responseUser = dbUsers.getUserById(userId);
        if (responseUser.isEmpty()) throw new NotFoundException();

        //Create the bid with given parameters
        var bid = new Bid(auctionId, userId, bidAmount);

        //Save the bid into its respective database
        dbBids.putBid(new BidDAO(bid));

        //Return the created bid's id (don't know if necessary since we only have list operation)
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

        //Check if the auction exists, if not, return NotFoundException
        var responseAuction = dbAuctions.getAuctionById(auctionId);
        if (responseAuction.isEmpty()) throw new NotFoundException();

        //Gather all the bids associated with given auction
        var responseBids = dbBids.listBidsByAuction(auctionId);

        //Construct the respective String with the collected data and return it
        StringBuilder res = new StringBuilder();
        for (BidDAO bid : responseBids){
            res.append(bid.toString());
        }
        return res.toString();
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
        //Check if the auction exists, if not, return NotFoundException
        var responseAuction = dbAuctions.getAuctionById(auctionId);
        if (responseAuction.isEmpty()) throw new NotFoundException();

        //Check if the user exists, if not, return NotFoundException
        var responseUser = dbUsers.getUserById(userId);
        if (responseUser.isEmpty()) throw new NotFoundException();

        //Create the question with given parameters
        var question = new Question(auctionId, userId, description);

        //Save the question into its respective database
        dbQuestions.putQuestion(new QuestionDAO(question));

        //Return the created question's id
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
        //Check if the auction exists, if not return NotFoundException
        var responseAuction = dbAuctions.getAuctionById(auctionId);
        if (responseAuction.isEmpty()) throw new NotFoundException();

        //Check if the question exists, if not return NotFoundException
        var responseQuestion = dbQuestions.getQuestionById(questionId);
        if (responseQuestion.isEmpty()) throw new NotFoundException();

        //Check if the user exists, if not return NotFoundException
        var responseUser = dbUsers.getUserById(userId);
        if (responseUser.isEmpty()) throw new NotFoundException();

        //Create the reply with given parameters
        var reply = new Reply(auctionId, questionId, userId, description);

        //Save the reply into its respective database
        dbReplies.putReply(new ReplyDAO(reply));

        //Return the created reply's id
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
        //Gather all the questions associated to the auctionId
        var response = dbQuestions.listQuestionsByAuctionId(auctionId);

        //Construct the respective String with collected data then return it
        StringBuilder res = new StringBuilder();
        for (QuestionDAO question : response)
            res.append(question.toString());
        return res.toString();
    }

    /**
     * Lists all the auctions that are considered to be "about to close"
     * @return String composed of each collected auction's JSON
     */
    @GET
    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public String listAuctionsAboutToClose(){
        // Collect the auctions that are considered to be "about to close"
        var response = dbAuctions.getAuctionsAboutToClose();

        //Construct the string with collected information and return it
        StringBuilder res = new StringBuilder();
        for (AuctionDAO auctionDAO : response)
            res.append(auctionDAO.toString());
        return res.toString();
    }

    /**
     * Lists all the auctions whose owner's identifier is the same as the one given
     * @param userId Identifier of the user
     * @return String composed of each collected auction's JSON
     */
    @GET
    @Path("/{" + USER_ID + "}/")
    @Produces(MediaType.TEXT_PLAIN)
    public String listAuctionsOfUser(@PathParam(USER_ID) String userId){
        // Collect the auctions where the userId is the same as the one given
        var response = dbAuctions.getAuctionsByUser(userId);

        //Construct the string with collected information and return it
        StringBuilder res = new StringBuilder();
        for (AuctionDAO auctionDAO : response)
            res.append(auctionDAO.toString());
        return res.toString();
    }
}
