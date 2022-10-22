package scc.services;

import java.util.List;
import java.util.Optional;

import scc.services.data.BidItem;
import scc.services.data.QuestionItem;
import scc.utils.Result;

public interface AuctionService {
    public static enum Error {
        BAD_REQUEST,
        USER_NOT_FOUND,
        AUCTION_NOT_FOUND,
        QUESTION_NOT_FOUND,
        QUESTION_ALREADY_REPLIED,
    }

    public static record CreateAuctionParams(
            String title,
            String description,
            String userId,
            long initialPrice,
            String endTime,
            Optional<byte[]> image) {
    }

    Result<String, Error> createAuction(CreateAuctionParams params);

    Result<Void, Error> deleteAuction(String auctionId);

    Result<List<String>, Error> listAuctionsOfUser(String userId);

    public static class UpdateAuctionOps {
        private String title;
        private String description;
        private byte[] image;

        public UpdateAuctionOps() {
        }

        public boolean shouldUpdateTitle() {
            return this.title != null;
        }

        public UpdateAuctionOps updateTitle(String title) {
            this.title = title;
            return this;
        }

        public String getTitle() {
            return this.title;
        }

        public boolean shouldUpdateDescription() {
            return this.description != null;
        }

        public UpdateAuctionOps updateDescription(String description) {
            this.description = description;
            return this;
        }

        public String getDescription() {
            return this.description;
        }

        public boolean shouldUpdateImage() {
            return this.image != null;
        }

        public UpdateAuctionOps updateImage(byte[] image) {
            this.image = image;
            return this;
        }

        public byte[] getImage() {
            return this.image;
        }
    }

    Result<Void, Error> updateAuction(String auctionId, UpdateAuctionOps ops);

    public static record CreateBidParams(
            String auctionId,
            String userId,
            long price) {
    }

    Result<String, Error> createBid(CreateBidParams params);

    Result<List<BidItem>, Error> listBids(String auctionId);

    public static record CreateQuestionParams(
            String auctionId,
            String userId,
            String question) {
    }

    Result<String, Error> createQuestion(CreateQuestionParams params);

    public static record CreateReplyParams(
            String auctionId,
            String questionId,
            String userId,
            String reply) {
    }

    Result<Void, Error> createReply(CreateReplyParams params);

    Result<List<QuestionItem>, Error> listQuestions(String auctionId);

    public static Result<Void, Error> validateCreateAuctionParams(CreateAuctionParams params) {
        if (params.title == null || params.title.isEmpty()) {
            return Result.err(Error.BAD_REQUEST);
        }
        if (params.description == null || params.description.isEmpty()) {
            return Result.err(Error.BAD_REQUEST);
        }
        if (params.userId == null || params.userId.isEmpty()) {
            return Result.err(Error.BAD_REQUEST);
        }
        if (params.initialPrice <= 0) {
            return Result.err(Error.BAD_REQUEST);
        }
        if (params.endTime == null || params.endTime.isEmpty()) {
            return Result.err(Error.BAD_REQUEST);
        }
        return Result.ok();
    }
}
