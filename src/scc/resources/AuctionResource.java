package scc.resources;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import scc.resources.data.BidDTO;
import scc.resources.data.QuestionDTO;
import scc.services.AuctionService;

@Path("/auction")
public class AuctionResource {
    private static final String AUCTION_ID = "auctionId";
    private static final String QUESTION_ID = "questionId";

    private final AuctionService service;

    public AuctionResource(AuctionService service) {
        this.service = service;
    }

    public static record CreateAuctionRequest(
            @JsonProperty(required = true) String title,
            @JsonProperty(required = true) String description,
            @JsonProperty(required = true) String userId,
            @JsonProperty(required = true) Long initialPrice,
            @JsonProperty(required = true) String endTime,
            String imageBase64) {
    }

    /**
     * Posts a new auction
     * 
     * @param request JSON which contains the necessary information to create an
     *                auction
     * @return Auction's generated identifier
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createAuction(CreateAuctionRequest request) {
        System.out.println("Received create auction request");
        System.out.println(request);

        var result = this.service.createAuction(new AuctionService.CreateAuctionParams(
                request.title(),
                request.description(),
                request.userId(),
                request.initialPrice().longValue(),
                request.endTime(),
                ResourceUtils.decodeBase64Nullable(request.imageBase64())));

        if (result.isError())
            ResourceUtils.throwError(result.error());

        return result.value();
    }

    public static record UpdateAuctionRequest(
            String title,
            String description,
            String imageBase64) {
    }

    /**
     * Updates an existing auction
     * 
     * @param auctionId Identifier of the auction
     * @param request   JSON which contains the info that wants to be changed
     */
    @PATCH
    @Path("/{" + AUCTION_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateAuction(@PathParam(AUCTION_ID) String auctionId, UpdateAuctionRequest request) {
        System.out.println("Received update auction request");
        System.out.println(request);

        var image = ResourceUtils.decodeBase64Nullable(request.imageBase64);

        var updateOps = new AuctionService.UpdateAuctionOps();
        if (request.title() != null)
            updateOps.updateTitle(request.title());
        if (request.description() != null)
            updateOps.updateDescription(request.description());
        image.ifPresent(updateOps::updateImage);

        var result = this.service.updateAuction(auctionId, updateOps);

        if (result.isError())
            ResourceUtils.throwError(result.error());
    }

    public static record CreateBidRequest(
            @JsonProperty(required = true) String userId,
            @JsonProperty(required = true) Long bid) {
    }

    /**
     * Creates a bid on an auction
     * 
     * @param auctionId Identifier of the auction
     * @param request   Arguments necessary to create a bid on the auction
     * @return Bid's generated identifier
     */
    @POST
    @Path("/{" + AUCTION_ID + "}/bid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createBid(@PathParam(AUCTION_ID) String auctionId, CreateBidRequest request) {
        System.out.println("Received create bid request");
        System.out.println(request);

        var result = this.service.createBid(new AuctionService.CreateBidParams(
                auctionId,
                request.userId(),
                request.bid().longValue()));

        if (result.isError())
            ResourceUtils.throwError(result.error());

        return result.value();
    }

    /**
     * Lists all the bids associated with an auction
     * 
     * @param auctionId Identifier of the auction
     * @return All the bids that were performed on the auction
     */
    @GET
    @Path("/{" + AUCTION_ID + "}/bid")
    @Produces(MediaType.APPLICATION_JSON)
    public List<BidDTO> listBids(@PathParam(AUCTION_ID) String auctionId) {
        System.out.println("Received list bids request");

        var result = this.service.listBids(auctionId);

        if (result.isError())
            ResourceUtils.throwError(result.error());

        return result.value().stream().map(BidDTO::from).toList();
    }

    public static record CreateQuestionRequest(
            @JsonProperty(required = true) String userId,
            @JsonProperty(required = true) String question) {
    }

    /**
     * Creates a question on an auction
     * 
     * @param auctionId Identifier of the auction
     * @param request   Arguments necessary to create a question in the auction
     * @return Question's generated identifier
     */
    @POST
    @Path("/{" + AUCTION_ID + "}/question")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createQuestion(@PathParam(AUCTION_ID) String auctionId, CreateQuestionRequest request) {
        System.out.println("Received create question request");
        System.out.println(request);

        var result = this.service.createQuestion(new AuctionService.CreateQuestionParams(
                auctionId,
                request.userId(),
                request.question()));

        if (result.isError())
            ResourceUtils.throwError(result.error());

        return result.value();
    }

    public static record CreateReplyRequest(
            @JsonProperty(required = true) String userId,
            @JsonProperty(required = true) String reply) {
    }

    /**
     * Creates a reply on a question done in an auction
     * 
     * @param auctionId  Identifier of the auction
     * @param questionId Identifier of the question
     * @param request    Arguments necessary for the execution of creating a reply
     * @return
     */
    @POST
    @Path("/{" + AUCTION_ID + "}/question/{" + QUESTION_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createReply(@PathParam(AUCTION_ID) String auctionId, @PathParam(QUESTION_ID) String questionId,
            CreateReplyRequest request) {
        System.out.println("Received create reply request");
        System.out.println(request);

        var result = this.service.createReply(new AuctionService.CreateReplyParams(
                auctionId,
                questionId,
                request.userId(),
                request.reply()));

        if (result.isError())
            ResourceUtils.throwError(result.error());
    }

    /**
     * Lists all the questions of an auction
     * 
     * @param auctionId Identifier of the auction
     * @return All the questions associated with the auction
     */
    @GET
    @Path("/{" + AUCTION_ID + "}/question")
    @Produces(MediaType.APPLICATION_JSON)
    public List<QuestionDTO> listQuestions(@PathParam(AUCTION_ID) String auctionId) {
        System.out.println("Received list questions request");

        var result = this.service.listQuestions(auctionId);

        if (result.isError())
            ResourceUtils.throwError(result.error());

        return result.value().stream().map(QuestionDTO::from).toList();
    }

    /**
     * Lists all the auctions that are considered to be "about to close"
     * 
     * @return String composed of each collected auction's JSON
     */
    @GET
    @Path("/")
    @Produces(MediaType.TEXT_PLAIN)
    public String listAuctionsAboutToClose() {
        // TODO
        throw new NotImplementedException();
    }

}
