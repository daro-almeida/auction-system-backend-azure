package scc.services;

import java.util.List;
import java.util.Optional;

import scc.services.data.BidItem;
import scc.services.data.QuestionItem;
import scc.utils.Result;

public interface AuctionService {
    record CreateAuctionParams(
            String title,
            String description,
            long initialPrice,
            String endTime,
            Optional<byte[]> image) {
    }

    Result<String, ServiceError> createAuction(CreateAuctionParams params, String sessionToken);

    Result<Void, ServiceError> deleteAuction(String auctionId);

    Result<List<String>, ServiceError> listAuctionsOfUser(String userId);

    class UpdateAuctionOps {
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

    Result<Void, ServiceError> updateAuction(String auctionId, UpdateAuctionOps ops, String sessionToken);

    public static record CreateBidParams(
            String auctionId,
            long price) {
    }

    Result<String, ServiceError> createBid(CreateBidParams params, String sessionToken);

    Result<List<BidItem>, ServiceError> listBids(String auctionId);

    public static record CreateQuestionParams(
            String auctionId,
            String question) {
    }

    Result<String, ServiceError> createQuestion(CreateQuestionParams params, String sessionToken);

    public static record CreateReplyParams(
            String auctionId,
            String questionId,
            String reply) {
    }

    Result<Void, ServiceError> createReply(CreateReplyParams params, String sessionToken);

    Result<List<QuestionItem>, ServiceError> listQuestions(String auctionId);

    static Result<Void, ServiceError> validateCreateAuctionParams(CreateAuctionParams params) {
        if (params.title == null || params.title.isEmpty()) {
            return Result.err(ServiceError.BAD_REQUEST);
        }
        if (params.description == null || params.description.isEmpty()) {
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
