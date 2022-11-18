package scc.rest;

import static scc.rest.ResourceUtils.SESSION_COOKIE;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.JsonProperty;

import scc.AuctionService;
import scc.UpdateAuctionOps;
import scc.rest.dto.AuctionDTO;
import scc.rest.dto.BidDTO;
import scc.rest.dto.QuestionDTO;
import scc.rest.dto.ReplyDTO;

@Path("/auction")
public class AuctionResource {
    private static final Logger logger = Logger.getLogger(AuctionResource.class.toString());

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
            @JsonProperty(required = true) String minimumPrice,
            @JsonProperty(required = true) String endTime,
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
     * @return Auction's generated identifier
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AuctionDTO createAuction(CreateAuctionRequest request, @CookieParam(SESSION_COOKIE) Cookie authentication) {
        logger.fine("POST /auction/ " + request);

        if (request.title == null || request.description == null || request.owner == null
                || request.minimumPrice == null || request.endTime == null)
            throw new BadRequestException("Missing required fields");

        var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);
        var createAuctionParams = new AuctionService.CreateAuctionParams(
                request.title,
                request.description,
                request.owner,
                Double.parseDouble(request.minimumPrice),
                ZonedDateTime.parse(request.endTime).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
                Optional.ofNullable(request.imageId).map(ResourceUtils::stringToMediaId));

        logger.fine("Creating auction with params: " + createAuctionParams);
        var result = this.service.createAuction(sessionToken, createAuctionParams);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var auctionItem = result.value();
        var auctionDto = AuctionDTO.from(auctionItem);
        logger.fine("Created auction: " + auctionDto);

        return auctionDto;
    }

    public static record UpdateAuctionRequest(
            String title,
            String description,
            String imageId) {
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
        logger.fine("PATCH /auction/" + auctionId + " " + request);

        var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);

        var ops = new UpdateAuctionOps();
        if (request.title != null)
            ops.updateTitle(request.title);
        if (request.description != null)
            ops.updateDescription(request.description);
        if (request.imageId != null)
            ops.updateImage(ResourceUtils.stringToMediaId(request.imageId));

        logger.fine("Updating auction with ops: " + ops);
        var result = this.service.updateAuction(sessionToken, auctionId, ops);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());
    }

    public record CreateBidRequest(
            @JsonProperty(required = true) String auctionId,
            @JsonProperty(required = true) String user,
            @JsonProperty(required = true) String value) {
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
    public BidDTO createBid(@PathParam(AUCTION_ID) String auctionId,
            CreateBidRequest request,
            @CookieParam(SESSION_COOKIE) Cookie authentication) {
        logger.fine("POST /auction/" + auctionId + "/bid " + request);

        if (!request.auctionId.equals(auctionId))
            throw new BadRequestException("Auction id in path and request body must match");

        var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);

        var createBidParams = new AuctionService.CreateBidParams(
                auctionId,
                request.user,
                Double.parseDouble(request.value));

        logger.fine("Creating bid with params: " + createBidParams);
        var result = this.service.createBid(sessionToken, createBidParams);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var bidItem = result.value();
        var bidDto = BidDTO.from(bidItem);
        logger.fine("Created bid: " + bidDto);

        return bidDto;
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
        logger.fine("GET /auction/" + auctionId + "/bid");

        if (auctionId == null)
            throw new BadRequestException("Auction id must be provided");

        var result = this.service.listAuctionBids(auctionId);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var bids = result.value();
        var bidsDto = bids.stream().map(BidDTO::from).collect(Collectors.toList());
        logger.fine("Found bids: " + bidsDto);

        return bidsDto;
    }

    public static record CreateQuestionRequest(
            @JsonProperty(required = true) String auctionId,
            @JsonProperty(required = true) String user,
            @JsonProperty(required = true) String text) {
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
    public QuestionDTO createQuestion(
            @PathParam(AUCTION_ID) String auctionId,
            CreateQuestionRequest request,
            @CookieParam(SESSION_COOKIE) Cookie authentication) {
        logger.fine("POST /auction/" + auctionId + "/question " + request);

        if (!request.auctionId.equals(auctionId))
            throw new BadRequestException("Auction id in path and request body must match");

        var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);

        var createQuestionParams = new AuctionService.CreateQuestionParams(
                auctionId,
                request.text);

        logger.fine("Creating question with params: " + createQuestionParams);
        var result = this.service.createQuestion(sessionToken, createQuestionParams);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var questionItem = result.value();
        var questionDto = QuestionDTO.from(questionItem);
        logger.fine("Created question: " + questionDto);

        return questionDto;
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
    @Path("/{" + AUCTION_ID + "}/question/{" + QUESTION_ID + "}/reply")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createReply(@PathParam(AUCTION_ID) String auctionId,
            @PathParam(QUESTION_ID) String questionId,
            CreateReplyRequest request,
            @CookieParam(SESSION_COOKIE) Cookie authentication) {
        logger.fine("POST /auction/" + auctionId + "/question/" + questionId + " " + request);

        if (auctionId == null || questionId == null)
            throw new BadRequestException("Auction id and question id must be provided");

        var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);

        var createReplyParams = new AuctionService.CreateReplyParams(
                auctionId,
                questionId,
                request.reply);

        logger.fine("Creating reply with params: " + createReplyParams);
        var result = this.service.createReply(sessionToken, createReplyParams);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var replyItem = result.value();

        return replyItem.getQuestionId();
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
        logger.fine("GET /auction/" + auctionId + "/question");

        if (auctionId == null)
            throw new BadRequestException("Auction id must be provided");

        var result = this.service.listAuctionQuestions(auctionId);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var questions = result.value();
        var questionsDto = questions.stream().map(QuestionDTO::from).collect(Collectors.toList());
        logger.fine("Found questions: " + questionsDto);

        return questionsDto;
    }

    /**
     * Lists all the auctions that are considered to be "about to close"
     * 
     * @return String composed of each collected auction's JSON
     */
    @GET
    @Path("/any/soon-to-close")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuctionDTO> listAuctionsAboutToClose() {
        logger.fine("GET /auction");

        var result = this.service.listAuctionsAboutToClose();
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var auctions = result.value();
        var auctionsDto = auctions.stream().map(AuctionDTO::from).collect(Collectors.toList());
        logger.fine("Found auctions: " + auctionsDto);

        return auctionsDto;
    }

    @GET
    @Path("/any/recent")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuctionDTO> listRecentAuctions() {
        logger.fine("GET /auction/any/recent");

        var result = this.service.listRecentAuctions();
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var auctions = result.value();
        var auctionsDto = auctions.stream().map(AuctionDTO::from).collect(Collectors.toList());
        logger.fine("Found auctions: " + auctionsDto);

        return auctionsDto;
    }

    @GET
    @Path("/any/popular")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuctionDTO> listPopularAuctions() {
        logger.fine("GET /auction/any/popular");

        var result = this.service.listPopularAuctions();
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var auctions = result.value();
        var auctionsDto = auctions.stream().map(AuctionDTO::from).collect(Collectors.toList());
        logger.fine("Found auctions: " + auctionsDto);

        return auctionsDto;
    }

    @GET
    @Path("/any/query")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuctionDTO> queryAuctions(@QueryParam("query") String query) {
        logger.fine("GET /auction/any/query?query=" + query);

        if (query == null)
            throw new BadRequestException("Query must be provided");

        var result = this.service.queryAuctions(query);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var auctions = result.value();
        var auctionsDto = auctions.stream().map(AuctionDTO::from).collect(Collectors.toList());
        logger.fine("Found auctions: " + auctionsDto);

        return auctionsDto;
    }

    @GET
    @Path("/{" + AUCTION_ID + "}/question/any/query")
    @Produces
    public List<QuestionDTO> queryQuestionsFromAuction(@PathParam(AUCTION_ID) String auctionId,
            @QueryParam("query") String query) {
        if (query == null)
            throw new BadRequestException("Query must be provided");

        var result = this.service.queryQuestionsFromAuction(auctionId, query);
        if (result.isError())
            ResourceUtils.throwError(result.error(), result.errorMessage());

        var questions = result.value();

        return questions.stream().map(QuestionDTO::from).collect(Collectors.toList());
    }

}
