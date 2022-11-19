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

        /**
         * Creates a new auction.
         * The title must be not empty.
         * The description must be not empty.
         * The owner must be a valid user id.
         * The starting price must be greater than 0.
         * The end time must be in the future.
         * 
         * @param token  the session token of the owner
         * @param params the parameters of the auction to create
         * @return the created auction
         */
        Result<AuctionItem, ServiceError> createAuction(SessionToken token, CreateAuctionParams params);

        /**
         * Gets the auction with the given id.
         * 
         * @param auctionId the id of the auction to get
         * @return the auction with the given id
         */
        Result<AuctionItem, ServiceError> getAuction(String auctionId);

        /**
         * Updates the auction with the given id.
         * 
         * @param token     the session token of the owner
         * @param auctionId the id of the auction to update
         * @param ops       the operations to perform
         * @return Ok if the auction was updated, Err otherwise
         */
        Result<Void, ServiceError> updateAuction(SessionToken token, String auctionId, UpdateAuctionOps ops);

        record CreateBidParams(
                        String auctionId,
                        String userId,
                        double price) {
        }

        /**
         * Creates a new bid.
         * The auction id must be a valid auction id.
         * The user id must be a valid user id.
         * The price must be greater than the current highest bid.
         * 
         * @param token  the session token of the user placing the bid
         * @param params the parameters of the bid to create
         * @return the created bid
         */
        Result<BidItem, ServiceError> createBid(SessionToken token, CreateBidParams params);

        /**
         * Lists all bids for the auction with the given id.
         * 
         * @param auctionId the id of the auction to list bids for
         * @return the bids for the auction with the given id
         */
        Result<List<BidItem>, ServiceError> listAuctionBids(String auctionId);

        record CreateQuestionParams(
                        String auctionId,
                        String question) {
        }

        /**
         * Creates a new question.
         * The auction id must be a valid auction id.
         * The question must be not empty.
         * 
         * @param token  the session token of the user asking the question
         * @param params the parameters of the question to create
         * @return the created question
         */
        Result<QuestionItem, ServiceError> createQuestion(SessionToken token, CreateQuestionParams params);

        record CreateReplyParams(
                        String auctionId,
                        String questionId,
                        String reply) {
        }

        /**
         * Creates a new reply.
         * The auction id must be a valid auction id.
         * The question id must be a valid question id.
         * The reply must be not empty.
         * Only the owner of the auction can create a reply.
         * 
         * @param token  the session token of the owner of the auction
         * @param params the parameters of the reply to create
         * @return the created reply
         */
        Result<ReplyItem, ServiceError> createReply(SessionToken token, CreateReplyParams params);

        /**
         * Lists all questions for the auction with the given id.
         * 
         * @param auctionId the id of the auction to list questions for
         * @return the questions for the auction with the given id
         */
        Result<List<QuestionItem>, ServiceError> listAuctionQuestions(String auctionId);

        /**
         * Lists all auctions created by the user with the given id.
         * 
         * @param userId the id of the user to list auctions for
         * @param open   whether to list only open auctions
         * @return the auctions created by the user with the given id
         */
        Result<List<AuctionItem>, ServiceError> listAuctionsOfUser(String userId, boolean open);

        /**
         * Lists all auctions that are currently open and the user with the given id has
         * placed a bid on.
         * 
         * @param userId the id of the user to list auctions for
         * @return the auctions that are currently open and the user with the given id
         *         has
         */
        Result<List<AuctionItem>, ServiceError> listAuctionsFollowedByUser(String userId);

        /**
         * Lists a number of auctions that are close to ending.
         * 
         * @return the auctions that are close to ending
         */
        Result<List<AuctionItem>, ServiceError> listAuctionsAboutToClose();

        /**
         * Lists a number of auctions that were recently created.
         * 
         * @return the auctions that were recently created
         */
        Result<List<AuctionItem>, ServiceError> listRecentAuctions();

        /**
         * Lists a number of auctions that are currently popular.
         * 
         * @return the auctions that are currently popular
         */
        Result<List<AuctionItem>, ServiceError> listPopularAuctions();

        /**
         * Lists a number of auctions that match the given search query.
         * 
         * @param query the search query
         * @return the auctions that match the given search query
         */
        Result<List<AuctionItem>, ServiceError> queryAuctions(String query);

        /**
         * Lists a number of questions from an auction that match the given search
         * query.
         * 
         * @param auctionId the id of the auction to list questions for
         * @param query     the search query
         * @return the questions from an auction that match the given search query
         */
        Result<List<QuestionItem>, ServiceError> queryQuestionsFromAuction(String auctionId, String query);
}
