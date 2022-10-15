package scc.memory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import scc.services.AuctionService;
import scc.services.data.BidItem;
import scc.services.data.QuestionItem;
import scc.services.data.ReplyItem;
import scc.utils.Result;

public class MemoryAuctionService implements AuctionService {
    private static class Auction {
        public String title;
        public String description;
        public String userId;
        public long initialPrice;
        public Optional<String> imageId;
        public HashSet<String> bids;
        public HashSet<String> questions;
    }

    private static class Bid {
        public String userId;
        public long price;
    }

    private static class Question {
        public String userId;
        public String question;
        public Optional<Reply> reply;
    }

    private static class Reply {
        public String userId;
        public String content;
    }

    private final MemoryUserService userService;
    private final MemoryMediaService mediaService;
    private final HashMap<String, Auction> auctions;
    private final HashMap<String, Bid> bids;
    private final HashMap<String, Question> questions;

    private int currentId;

    public MemoryAuctionService(MemoryUserService userService, MemoryMediaService mediaService) {
        this.userService = userService;
        this.mediaService = mediaService;
        this.auctions = new HashMap<>();
        this.bids = new HashMap<>();
        this.questions = new HashMap<>();
        this.currentId = 0;
    }

    @Override
    public synchronized Result<String, Error> createAuction(CreateAuctionParams params) {
        if (!this.userService.userExists(params.userId())) {
            return Result.error(Error.USER_NOT_FOUND);
        }

        var auction = new Auction();
        auction.title = params.title();
        auction.description = params.description();
        auction.userId = params.userId();
        auction.initialPrice = params.initialPrice();
        auction.imageId = params.image().map(this.mediaService::uploadMedia);
        auction.bids = new HashSet<>();
        auction.questions = new HashSet<>();

        var id = this.generateId();
        this.auctions.put(id, auction);
        return Result.ok(id);
    }

    @Override
    public synchronized Result<Void, Error> deleteAuction(String auctionId) {
        var auction = this.auctions.remove(auctionId);
        if (auction == null) {
            return Result.error(Error.AUCTION_NOT_FOUND);
        }

        if (auction.imageId.isPresent()) {
            this.mediaService.deleteMedia(auction.imageId.get());
        }

        return Result.ok();
    }

    @Override
    public synchronized Result<Void, Error> updateAuction(String auctionId, UpdateAuctionOps ops) {
        var auction = this.auctions.get(auctionId);
        if (auction == null) {
            return Result.error(Error.AUCTION_NOT_FOUND);
        }

        if (ops.shouldUpdateTitle()) {
            auction.title = ops.getTitle();
        }

        if (ops.shouldUpdateDescription()) {
            auction.description = ops.getDescription();
        }

        if (ops.shouldUpdateImage()) {
            if (auction.imageId.isPresent()) {
                this.mediaService.deleteMedia(auction.imageId.get());
            }
            auction.imageId = Optional.of(this.mediaService.uploadMedia(ops.getImage()));
        }

        return Result.ok();
    }

    @Override
    public synchronized Result<String, Error> createBid(CreateBidParams params) {
        var auction = this.auctions.get(params.auctionId());
        if (auction == null) {
            return Result.error(Error.AUCTION_NOT_FOUND);
        }

        if (!this.userService.userExists(params.userId())) {
            return Result.error(Error.USER_NOT_FOUND);
        }

        var bid = new Bid();
        bid.userId = params.userId();
        bid.price = params.price();

        var id = this.generateId();
        this.bids.put(id, bid);
        auction.bids.add(id);
        return Result.ok(id);
    }

    @Override
    public synchronized Result<List<BidItem>, Error> listBids(String auctionId) {
        var auction = this.auctions.get(auctionId);
        if (auction == null) {
            return Result.error(Error.AUCTION_NOT_FOUND);
        }

        var bids = auction.bids.stream().map(id -> {
            var bid = this.bids.get(id);
            return new BidItem(id, bid.userId, bid.price);
        }).toList();

        return Result.ok(bids);
    }

    @Override
    public synchronized Result<String, Error> createQuestion(CreateQuestionParams params) {
        var auction = this.auctions.get(params.auctionId());
        if (auction == null) {
            return Result.error(Error.AUCTION_NOT_FOUND);
        }

        if (!this.userService.userExists(params.userId())) {
            return Result.error(Error.USER_NOT_FOUND);
        }

        var question = new Question();
        question.userId = params.userId();
        question.question = params.question();
        question.reply = Optional.empty();

        var id = this.generateId();
        auction.questions.add(id);
        return Result.ok(id);
    }

    @Override
    public synchronized Result<Void, Error> createReply(CreateReplyParams params) {
        var auction = this.auctions.get(params.auctionId());
        if (auction == null) {
            return Result.error(Error.AUCTION_NOT_FOUND);
        }

        if (!this.userService.userExists(params.userId())) {
            return Result.error(Error.USER_NOT_FOUND);
        }

        if (!auction.questions.contains(params.questionId())) {
            return Result.error(Error.QUESTION_NOT_FOUND);
        }

        var question = this.questions.get(params.questionId());
        if (question.reply.isPresent()) {
            return Result.error(Error.QUESTION_ALREADY_REPLIED);
        }

        var reply = new Reply();
        reply.userId = params.userId();
        reply.content = params.reply();
        question.reply = Optional.of(reply);

        return Result.ok();
    }

    @Override
    public synchronized Result<List<QuestionItem>, Error> listQuestions(String auctionId) {
        var auction = this.auctions.get(auctionId);
        if (auction == null) {
            return Result.error(Error.AUCTION_NOT_FOUND);
        }

        var questions = auction.questions.stream().map(id -> {
            var question = this.questions.get(id);
            return new QuestionItem(id, question.userId, question.question, question.reply.map(reply -> {
                return new ReplyItem(reply.userId, reply.content);
            }));
        }).collect(Collectors.toList());

        return Result.ok(questions);
    }

    private String generateId() {
        return Integer.toString(this.currentId++);
    }

}
