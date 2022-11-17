package scc.memory;

import scc.services.AuctionService;
import scc.services.ServiceError;
import scc.services.data.AuctionItem;
import scc.services.data.BidItem;
import scc.services.data.QuestionItem;
import scc.utils.Result;

import java.util.List;

public class MemoryAuctionService implements AuctionService {

    public MemoryAuctionService(MemoryUserService userService, MemoryMediaService mediaService) {
    }

    @Override
    public Result<AuctionItem, ServiceError> createAuction(CreateAuctionParams params, String sessionToken) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<Void, ServiceError> deleteAuction(String auctionId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listAuctionsOfUser(String userId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<Void, ServiceError> updateAuction(String auctionId, UpdateAuctionOps ops, String sessionToken) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<BidItem, ServiceError> createBid(CreateBidParams params, String sessionToken) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<List<BidItem>, ServiceError> listBids(String auctionId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<QuestionItem, ServiceError> createQuestion(CreateQuestionParams params, String sessionToken) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<Void, ServiceError> createReply(CreateReplyParams params, String sessionToken) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<List<QuestionItem>, ServiceError> listQuestions(String auctionId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listAuctionsAboutToClose() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listRecentAuctions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result<List<AuctionItem>, ServiceError> listPopularAuctions() {
        // TODO Auto-generated method stub
        return null;
    }

}

// public class MemoryAuctionService implements AuctionService {
// private static class Auction {
// public String title;
// public String description;
// public String userId;
// public long initialPrice;
// public Optional<String> imageId;
// public HashSet<String> bids;
// public HashSet<String> questions;
// }

// private static class Bid {
// public String userId;
// public long price;
// }

// private static class Question {
// public String userId;
// public String question;
// public Optional<Reply> reply;
// }

// private static class Reply {
// public String userId;
// public String content;
// }

// private final MemoryUserService userService;
// private final MemoryMediaService mediaService;
// private final HashMap<String, Auction> auctions;
// private final HashMap<String, Bid> bids;
// private final HashMap<String, Question> questions;

// private int currentId;

// public MemoryAuctionService(MemoryUserService userService, MemoryMediaService
// mediaService) {
// this.userService = userService;
// this.mediaService = mediaService;
// this.auctions = new HashMap<>();
// this.bids = new HashMap<>();
// this.questions = new HashMap<>();
// this.currentId = 0;
// }

// @Override
// public synchronized Result<String, ServiceError>
// createAuction(CreateAuctionParams params, String sessionToken) {
// if (!this.userService.userExists(params.userId())) {
// return Result.err(ServiceError.USER_NOT_FOUND);
// }

// var auction = new Auction();
// auction.title = params.title();
// auction.description = params.description();
// auction.userId = params.userId();
// auction.initialPrice = params.initialPrice();
// auction.imageId = params.image().map(this.mediaService::uploadAuctionMedia);
// auction.bids = new HashSet<>();
// auction.questions = new HashSet<>();

// var id = this.generateId();
// this.auctions.put(id, auction);
// return Result.ok(id);
// }

// @Override
// public synchronized Result<Void, ServiceError> deleteAuction(String
// auctionId) {
// var auction = this.auctions.remove(auctionId);
// if (auction == null) {
// return Result.err(ServiceError.AUCTION_NOT_FOUND);
// }

// if (auction.imageId.isPresent()) {
// this.mediaService.deleteMedia(auction.imageId.get());
// }

// return Result.ok();
// }

// @Override
// public synchronized Result<List<String>, ServiceError>
// listAuctionsOfUser(String userId) {
// return Result.ok(this.auctions.entrySet().stream()
// .filter(entry -> entry.getValue().userId.equals(userId))
// .map(entry -> entry.getKey())
// .collect(Collectors.toList()));
// }

// @Override
// public synchronized Result<Void, ServiceError> updateAuction(String
// auctionId, UpdateAuctionOps ops,
// String sessionToken) {
// var auction = this.auctions.get(auctionId);
// if (auction == null) {
// return Result.err(ServiceError.AUCTION_NOT_FOUND);
// }

// if (ops.shouldUpdateTitle()) {
// auction.title = ops.getTitle();
// }

// if (ops.shouldUpdateDescription()) {
// auction.description = ops.getDescription();
// }

// if (ops.shouldUpdateImage()) {
// if (auction.imageId.isPresent()) {
// this.mediaService.deleteMedia(auction.imageId.get());
// }
// auction.imageId =
// Optional.of(this.mediaService.uploadAuctionMedia(ops.getImage()));
// }

// return Result.ok();
// }

// @Override
// public synchronized Result<String, ServiceError> createBid(CreateBidParams
// params, String sessionToken) {
// var auction = this.auctions.get(params.auctionId());
// if (auction == null) {
// return Result.err(ServiceError.AUCTION_NOT_FOUND);
// }

// if (!this.userService.userExists(params.userId())) {
// return Result.err(ServiceError.USER_NOT_FOUND);
// }

// var bid = new Bid();
// bid.userId = params.userId();
// bid.price = params.price();

// var id = this.generateId();
// this.bids.put(id, bid);
// auction.bids.add(id);
// return Result.ok(id);
// }

// @Override
// public synchronized Result<List<BidItem>, ServiceError> listBids(String
// auctionId) {
// var auction = this.auctions.get(auctionId);
// if (auction == null) {
// return Result.err(ServiceError.AUCTION_NOT_FOUND);
// }

// var bids = auction.bids.stream().map(id -> {
// var bid = this.bids.get(id);
// return new BidItem(id, bid.userId, bid.price);
// }).toList();

// return Result.ok(bids);
// }

// @Override
// public synchronized Result<String, ServiceError>
// createQuestion(CreateQuestionParams params, String sessionToken) {
// var auction = this.auctions.get(params.auctionId());
// if (auction == null) {
// return Result.err(ServiceError.AUCTION_NOT_FOUND);
// }

// if (!this.userService.userExists(params.userId())) {
// return Result.err(ServiceError.USER_NOT_FOUND);
// }

// var question = new Question();
// question.userId = params.userId();
// question.question = params.question();
// question.reply = Optional.empty();

// var id = this.generateId();
// auction.questions.add(id);
// return Result.ok(id);
// }

// @Override
// public synchronized Result<Void, ServiceError> createReply(CreateReplyParams
// params, String sessionToken) {
// var auction = this.auctions.get(params.auctionId());
// if (auction == null) {
// return Result.err(ServiceError.AUCTION_NOT_FOUND);
// }

// if (!this.userService.userExists(params.userId())) {
// return Result.err(ServiceError.USER_NOT_FOUND);
// }

// if (!auction.questions.contains(params.questionId())) {
// return Result.err(ServiceError.QUESTION_NOT_FOUND);
// }

// var question = this.questions.get(params.questionId());
// if (question.reply.isPresent()) {
// return Result.err(ServiceError.QUESTION_ALREADY_REPLIED);
// }

// var reply = new Reply();
// reply.userId = params.userId();
// reply.content = params.reply();
// question.reply = Optional.of(reply);

// return Result.ok();
// }

// @Override
// public synchronized Result<List<QuestionItem>, ServiceError>
// listQuestions(String auctionId) {
// var auction = this.auctions.get(auctionId);
// if (auction == null) {
// return Result.err(ServiceError.AUCTION_NOT_FOUND);
// }

// var questions = auction.questions.stream().map(id -> {
// var question = this.questions.get(id);
// return new QuestionItem(id, question.userId, question.question,
// question.reply.map(reply -> {
// return new ReplyItem(reply.userId, reply.content);
// }));
// }).collect(Collectors.toList());

// return Result.ok(questions);
// }

// private String generateId() {
// return Integer.toString(this.currentId++);
// }

// }