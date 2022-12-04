package scc.rest;

import static scc.rest.ResourceUtils.SESSION_COOKIE;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import scc.AuctionService;
import scc.PagingWindow;
import scc.ServiceFactory;
import scc.UpdateAuctionOps;
import scc.rest.dto.AuctionDTO;
import scc.rest.dto.BidDTO;
import scc.rest.dto.QuestionDTO;

@Path("/auction")
public class AuctionResource {
    private static final Logger logger = Logger.getLogger(AuctionResource.class.toString());

    private static final String AUCTION_ID = "auctionId";
    private static final String QUESTION_ID = "questionId";

    private final ServiceFactory<AuctionService> factory;

    public AuctionResource(ServiceFactory<AuctionService> factory) {
        this.factory = factory;
    }

    @GET
    @Path("/{" + AUCTION_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    public AuctionDTO getAuction(@PathParam(AUCTION_ID) String auctionId) throws Exception {
        logger.fine("GET /auction/" + auctionId);
        try (var service = this.factory.createService()) {
            var auction = service.getAuction(auctionId);
            return AuctionDTO.from(auction);
        }
    }

    public record CreateAuctionRequest(
            @JsonProperty(required = true) String title,
            @JsonProperty(required = true) String description,
            @JsonProperty(required = true) String owner,
            @JsonProperty(required = true) String minimumPrice,
            @JsonProperty(required = true) String endTime,
            String imageId,
            String status) {
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
    public AuctionDTO createAuction(CreateAuctionRequest request, @CookieParam(SESSION_COOKIE) Cookie authentication)
            throws Exception {
        logger.fine("POST /auction/ " + request);

        if (request.title == null || request.description == null || request.owner == null
                || request.minimumPrice == null || request.endTime == null)
            throw new BadRequestException("Missing required fields");

        try (var service = this.factory.createService()) {
            var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);
            var createAuctionParams = new AuctionService.CreateAuctionParams(
                    request.title,
                    request.description,
                    request.owner,
                    Double.parseDouble(request.minimumPrice),
                    ZonedDateTime.parse(request.endTime).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
                    Optional.ofNullable(request.imageId).map(ResourceUtils::stringToMediaId));

            logger.fine("Creating auction with params: " + createAuctionParams);
            var auction = service.createAuction(sessionToken, createAuctionParams);
            var auctionDto = AuctionDTO.from(auction);
            logger.fine("Created auction: " + auctionDto);

            return auctionDto;
        }
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
            @CookieParam(SESSION_COOKIE) Cookie authentication) throws Exception {
        logger.fine("PATCH /auction/" + auctionId + " " + request);

        try (var service = this.factory.createService()) {
            var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);

            var ops = new UpdateAuctionOps();
            if (request.title != null)
                ops.updateTitle(request.title);
            if (request.description != null)
                ops.updateDescription(request.description);
            if (request.imageId != null)
                ops.updateImage(ResourceUtils.stringToMediaId(request.imageId));

            logger.fine("Updating auction with ops: " + ops);
            service.updateAuction(sessionToken, auctionId, ops);
        }
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
            @CookieParam(SESSION_COOKIE) Cookie authentication) throws Exception {
        logger.fine("POST /auction/" + auctionId + "/bid " + request);

        if (!request.auctionId.equals(auctionId))
            throw new BadRequestException("Auction id in path and request body must match");

        try (var service = this.factory.createService()) {
            var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);

            var createBidParams = new AuctionService.CreateBidParams(
                    auctionId,
                    request.user,
                    Double.parseDouble(request.value));

            logger.fine("Creating bid with params: " + createBidParams);
            var bid = service.createBid(sessionToken, createBidParams);
            var bidDto = BidDTO.from(bid);
            logger.fine("Created bid: " + bidDto);

            return bidDto;
        }
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
    public List<BidDTO> listBids(@PathParam(AUCTION_ID) String auctionId) throws Exception {
        logger.fine("GET /auction/" + auctionId + "/bid");

        if (auctionId == null)
            throw new BadRequestException("Auction id must be provided");

        try (var service = this.factory.createService()) {
            var bids = service.listAuctionBids(auctionId, new PagingWindow(0, 20));
            var bidsDto = bids.stream().map(BidDTO::from).collect(Collectors.toList());
            logger.fine("Found bids: " + bidsDto);

            return bidsDto;
        }
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
            @CookieParam(SESSION_COOKIE) Cookie authentication) throws Exception {
        logger.fine("POST /auction/" + auctionId + "/question " + request);

        if (!request.auctionId.equals(auctionId))
            throw new BadRequestException("Auction id in path and request body must match");

        try (var service = this.factory.createService()) {
            var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);

            var createQuestionParams = new AuctionService.CreateQuestionParams(
                    auctionId,
                    request.text);

            logger.fine("Creating question with params: " + createQuestionParams);
            var question = service.createQuestion(sessionToken, createQuestionParams);
            var questionDto = QuestionDTO.from(question);
            logger.fine("Created question: " + questionDto);

            return questionDto;
        }
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
            @CookieParam(SESSION_COOKIE) Cookie authentication) throws Exception {
        logger.fine("POST /auction/" + auctionId + "/question/" + questionId + " " + request);

        if (auctionId == null || questionId == null)
            throw new BadRequestException("Auction id and question id must be provided");

        try (var service = this.factory.createService()) {
            var sessionToken = ResourceUtils.sessionTokenOrFail(authentication);

            var createReplyParams = new AuctionService.CreateReplyParams(
                    auctionId,
                    questionId,
                    request.reply);

            logger.fine("Creating reply with params: " + createReplyParams);
            var reply = service.createReply(sessionToken, createReplyParams);
            return reply.getQuestionId();
        }
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
    public List<QuestionDTO> listQuestions(
            @PathParam(AUCTION_ID) String auctionId,
            @QueryParam("skip") @DefaultValue("0") int skip,
            @QueryParam("limit") @DefaultValue("20") int limit) throws Exception {
        logger.fine("GET /auction/" + auctionId + "/question");

        if (auctionId == null)
            throw new BadRequestException("Auction id must be provided");

        try (var service = this.factory.createService()) {
            var window = new PagingWindow(skip, limit);
            var questions = service.listAuctionQuestions(auctionId, window);
            var questionsDto = questions.stream().map(QuestionDTO::from).collect(Collectors.toList());
            logger.fine("Found questions: " + questionsDto);
            return questionsDto;
        }
    }

    /**
     * Lists all the auctions that are considered to be "about to close"
     * 
     * @return String composed of each collected auction's JSON
     */
    @GET
    @Path("/any/soon-to-close")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuctionDTO> listAuctionsAboutToClose() throws Exception {
        logger.fine("GET /auction/any/soon-to-close");

        try (var service = this.factory.createService()) {
            var auctions = service.listAuctionsAboutToClose();
            var auctionsDto = auctions.stream().map(AuctionDTO::from).collect(Collectors.toList());
            logger.fine("Found auctions: " + auctionsDto);
            return auctionsDto;
        }
    }

    @GET
    @Path("/any/recent")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuctionDTO> listRecentAuctions() throws Exception {
        logger.fine("GET /auction/any/recent");

        try (var service = this.factory.createService()) {
            var auctions = service.listRecentAuctions();
            var auctionsDto = auctions.stream().map(AuctionDTO::from).collect(Collectors.toList());
            logger.fine("Found auctions: " + auctionsDto);
            return auctionsDto;
        }
    }

    @GET
    @Path("/any/popular")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuctionDTO> listPopularAuctions() throws Exception {
        logger.fine("GET /auction/any/popular");

        try (var service = this.factory.createService()) {
            var auctions = service.listPopularAuctions();
            var auctionsDto = auctions.stream().map(AuctionDTO::from).collect(Collectors.toList());
            logger.fine("Found auctions: " + auctionsDto);
            return auctionsDto;
        }
    }

}
