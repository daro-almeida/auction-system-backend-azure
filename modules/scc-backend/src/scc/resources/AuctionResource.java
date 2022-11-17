package scc.resources;

import java.util.List;

import com.azure.search.documents.models.SearchResult;
import jakarta.annotation.Resource;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import org.apache.commons.lang3.NotImplementedException;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.ws.rs.core.MediaType;
import scc.resources.data.AuctionDTO;
import scc.resources.data.BidDTO;
import scc.resources.data.QuestionDTO;
import scc.services.AuctionService;
import scc.services.data.AuctionItem;

import static scc.resources.ResourceUtils.SESSION_COOKIE;

@Path("/auction")
public class AuctionResource {
    private static final String AUCTION_ID = "auctionId";
    private static final String QUESTION_ID = "questionId";

    private final AuctionService service;

    public AuctionResource(AuctionService service) {
        this.service = service;
    }

    public record CreateAuctionRequest(
            @JsonProperty(required = true) String title,
            @JsonProperty(required = true) String description,
            @JsonProperty(required = true) String owner,
            @JsonProperty(required = true) Long initialPrice,
            @JsonProperty(required = true) String endTime,
            String imageBase64,
            String imageId) {
    }

    /**
     * Posts a new auction
     * 
     * @param request        JSON which contains the necessary information to create
     *                       an
     *                       auction
     * @param authentication Cookie related to the user being "logged" in the
     *                       application
     * @return Auction's generated identifier // TODO change do DTO
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createAuction(CreateAuctionRequest request, @CookieParam(SESSION_COOKIE) Cookie authentication) {
        System.out.println("Received create auction request");
        System.out.println(request);
        var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);
        var result = this.service.createAuction(new AuctionService.CreateAuctionParams(
                request.title(),
                request.description(),
                request.initialPrice().longValue(),
                request.endTime(),
                ResourceUtils.decodeBase64Nullable(request.imageBase64())),
                sessionToken);

        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        return result.value().getId();
    }

    public static record UpdateAuctionRequest(
            String title,
            String description,
            String imageBase64) {
    }

    /**
     * Updates an existing auction
     * 
     * @param auctionId      Identifier of the auction
     * @param request        JSON which contains the info that wants to be changed
     * @param authentication Cookie related to the user being "logged" in the
     *                       application
     */
    @PATCH
    @Path("/{" + AUCTION_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateAuction(@PathParam(AUCTION_ID) String auctionId,
            UpdateAuctionRequest request,
            @CookieParam(SESSION_COOKIE) Cookie authentication) {
        System.out.println("Received update auction request");
        System.out.println(request);

        var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);
        var image = ResourceUtils.decodeBase64Nullable(request.imageBase64);

        var updateOps = new AuctionService.UpdateAuctionOps();
        if (request.title() != null)
            updateOps.updateTitle(request.title());
        if (request.description() != null)
            updateOps.updateDescription(request.description());
        image.ifPresent(updateOps::updateImage);

        var result = this.service.updateAuction(auctionId, updateOps, sessionToken);

        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());
    }

    public record CreateBidRequest(
        @JsonProperty(required = true) String userId,
        @JsonProperty(required = true) Long bid) {
    }

    /**
     * Creates a bid on an auction
     * 
     * @param auctionId      Identifier of the auction
     * @param request        Arguments necessary to create a bid on the auction
     * @param authentication Cookie related to the user being "logged" in the
     *                       application
     * @return Bid's generated identifier
     */
    @POST
    @Path("/{" + AUCTION_ID + "}/bid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createBid(@PathParam(AUCTION_ID) String auctionId,
            CreateBidRequest request,
            @CookieParam(SESSION_COOKIE) Cookie authentication) { // TODO change do DTO
        System.out.println("Received create bid request");
        System.out.println(request);

        var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);
        var result = this.service.createBid(new AuctionService.CreateBidParams(
                auctionId,
                request.bid().longValue()),
                sessionToken);

        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        return result.value().getId();
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
            ResourceUtils.throwError(result.error(), result.errorMessage());

        return result.value().stream().map(BidDTO::from).toList();
    }

    public static record CreateQuestionRequest(
            @JsonProperty(required = true) String question) {
    }

    /**
     * Creates a question on an auction
     * 
     * @param auctionId      Identifier of the auction
     * @param request        Arguments necessary to create a question in the auction
     * @param authentication Cookie related to the user being "logged" in the
     *                       application
     * @return Question's generated identifier
     */
    @POST
    @Path("/{" + AUCTION_ID + "}/question")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createQuestion(@PathParam(AUCTION_ID) String auctionId,
            CreateQuestionRequest request,
            @CookieParam(SESSION_COOKIE) Cookie authentication) { //TODO change to DTO
        System.out.println("Received create question request");
        System.out.println(request);

        var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);
        var result = this.service.createQuestion(new AuctionService.CreateQuestionParams(
                auctionId,
                request.question()),
                sessionToken);

        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        return result.value().getId();
    }

    public static record CreateReplyRequest(
            @JsonProperty(required = true) String reply) {
    }

    /**
     * Creates a reply on a question done in an auction
     * 
     * @param auctionId      Identifier of the auction
     * @param questionId     Identifier of the question
     * @param request        Arguments necessary for the execution of creating a
     *                       reply
     * @param authentication Cookie related to the user being "logged" in the
     *                       application
     * @return
     */
    @POST
    @Path("/{" + AUCTION_ID + "}/question/{" + QUESTION_ID + "}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createReply(@PathParam(AUCTION_ID) String auctionId,
            @PathParam(QUESTION_ID) String questionId,
            CreateReplyRequest request,
            @CookieParam(SESSION_COOKIE) Cookie authentication) {
        System.out.println("Received create reply request");
        System.out.println(request);

        var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);
        var result = this.service.createReply(new AuctionService.CreateReplyParams(
                auctionId,
                questionId,
                request.reply()),
                sessionToken);

        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());
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
            ResourceUtils.throwError(result.error(), result.errorMessage());

        return result.value().stream().map(QuestionDTO::from).toList();
    }

    /**
     * Lists all the auctions that are considered to be "about to close"
     * 
     * @return String composed of each collected auction's JSON
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuctionDTO> listAuctionsAboutToClose() {
        System.out.println("Received list auctions about to close request");

        var result = this.service.listAuctionsAboutToClose();

        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        return result.value().stream().map(AuctionDTO::from).toList();
    }

    @GET
    @Path("/recent")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuctionDTO> listRecentAuctions() {
        System.out.println("Received list recent auctions request");

        var result = this.service.listRecentAuctions();

        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        return result.value().stream().map(AuctionDTO::from).toList();
    }

    @GET
    @Path("/popular")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuctionDTO> listPopularAuctions(){
        System.out.println("Received list popular auctions request");

        var result = this.service.listPopularAuctions();

        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        return result.value().stream().map(AuctionDTO::from).toList();
    }

    @GET
    @Path("/any")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SearchResult> listQueryAuctions(@QueryParam("query") String query){
        System.out.printf("Received list auctions request with query %s\n", query);

        var result = this.service.listQueryAuctions(query);

        if(result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        return result.value().stream().toList();
    }

}
