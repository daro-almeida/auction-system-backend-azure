package scc.services;

import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.Cookie;
import scc.services.data.BidItem;
import scc.services.data.QuestionItem;
import scc.utils.Result;

public interface AuctionService {
    public static record CreateAuctionParams(
            String title,
            String description,
            String userId,
            long initialPrice,
            String endTime,
            Optional<byte[]> image) {
    }

    Result<String, ServiceError> createAuction(CreateAuctionParams params, Cookie auth);

    Result<Void, ServiceError> deleteAuction(String auctionId);

    Result<List<String>, ServiceError> listAuctionsOfUser(String userId);

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

    Result<Void, ServiceError> updateAuction(String auctionId, UpdateAuctionOps ops, Cookie auth);

    public static record CreateBidParams(
            String auctionId,
            String userId,
            long price) {
    }

    Result<String, ServiceError> createBid(CreateBidParams params, Cookie auth);

    Result<List<BidItem>, ServiceError> listBids(String auctionId);

    public static record CreateQuestionParams(
            String auctionId,
            String userId,
            String question) {
    }

    Result<String, ServiceError> createQuestion(CreateQuestionParams params, Cookie auth);

    public static record CreateReplyParams(
            String auctionId,
            String questionId,
            String userId,
            String reply) {
    }

    Result<Void, ServiceError> createReply(CreateReplyParams params, Cookie auth);

    Result<List<QuestionItem>, ServiceError> listQuestions(String auctionId);

    public static Result<Void, ServiceError> validateCreateAuctionParams(CreateAuctionParams params) {
        if (params.title == null || params.title.isEmpty()) {
            return Result.err(ServiceError.BAD_REQUEST);
        }
        if (params.description == null || params.description.isEmpty()) {
            return Result.err(ServiceError.BAD_REQUEST);
        }
        if (params.userId == null || params.userId.isEmpty()) {
            return Result.err(ServiceError.BAD_REQUEST);
        }
        if (params.initialPrice <= 0) {
            return Result.err(ServiceError.BAD_REQUEST);
        }
        if (params.endTime == null || params.endTime.isEmpty()) {
            return Result.err(ServiceError.BAD_REQUEST);
        }
        return Result.ok();
    }
}
