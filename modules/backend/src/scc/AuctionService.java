package scc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import scc.item.AuctionItem;
import scc.item.BidItem;
import scc.item.QuestionItem;
import scc.item.ReplyItem;

public interface AuctionService {
        record CreateAuctionParams(
                        String title,
                        String description,
                        String owner,
                        double startingPrice,
                        LocalDateTime endTime,
                        Optional<MediaId> mediaId) {
        }

        Result<AuctionItem, ServiceError> createAuction(SessionToken token, CreateAuctionParams params);

        Result<AuctionItem, ServiceError> getAuction(String auctionId);

        Result<Void, ServiceError> updateAuction(SessionToken token, String auctionId, UpdateAuctionOps ops);

        record CreateBidParams(
                        String auctionId,
                        String userId,
                        double price) {
        }

        Result<BidItem, ServiceError> createBid(SessionToken token, CreateBidParams params);

        Result<List<BidItem>, ServiceError> listAuctionBids(String auctionId);

        record CreateQuestionParams(
                        String auctionId,
                        String question) {
        }

        Result<QuestionItem, ServiceError> createQuestion(SessionToken token, CreateQuestionParams params);

        record CreateReplyParams(
                        String auctionId,
                        String questionId,
                        String reply) {
        }

        Result<ReplyItem, ServiceError> createReply(SessionToken token, CreateReplyParams params);

        Result<List<QuestionItem>, ServiceError> listAuctionQuestions(String auctionId);

        Result<List<AuctionItem>, ServiceError> listAuctionsOfUser(String userId, boolean open);

        Result<List<AuctionItem>, ServiceError> listAuctionsFollowedByUser(String userId);

        Result<List<AuctionItem>, ServiceError> listAuctionsAboutToClose();

        Result<List<AuctionItem>, ServiceError> listRecentAuctions();

        Result<List<AuctionItem>, ServiceError> listPopularAuctions();

        Result<List<AuctionItem>, ServiceError> queryAuctions(String query);
}
