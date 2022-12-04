package scc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import scc.exception.ServiceException;
import scc.item.AuctionItem;
import scc.item.BidItem;
import scc.item.QuestionItem;
import scc.item.ReplyItem;

public interface AuctionService extends AutoCloseable {
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
        AuctionItem createAuction(SessionToken token, CreateAuctionParams params) throws ServiceException;

        /**
         * Gets the auction with the given id.
         * 
         * @param auctionId the id of the auction to get
         * @return the auction with the given id
         */
        AuctionItem getAuction(String auctionId) throws ServiceException;

        /**
         * Updates the auction with the given id.
         * 
         * @param token     the session token of the owner
         * @param auctionId the id of the auction to update
         * @param ops       the operations to perform
         * @return Ok if the auction was updated, Err otherwise
         */
        void updateAuction(SessionToken token, String auctionId, UpdateAuctionOps ops) throws ServiceException;

        record CreateBidParams(
                        String auctionId,
                        String username,
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
        BidItem createBid(SessionToken token, CreateBidParams params) throws ServiceException;

        /**
         * Lists all bids for the auction with the given id.
         * 
         * @param auctionId the id of the auction to list bids for
         * @param skip      the number of bids to skip
         * @param limit     the maximum number of bids to return
         * @return the bids for the auction with the given id
         */
        List<BidItem> listAuctionBids(String auctionId, PagingWindow window) throws ServiceException;

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
        QuestionItem createQuestion(SessionToken token, CreateQuestionParams params) throws ServiceException;

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
        ReplyItem createReply(SessionToken token, CreateReplyParams params) throws ServiceException;

        /**
         * Lists all questions for the auction with the given id.
         * 
         * @param auctionId the id of the auction to list questions for
         * @return the questions for the auction with the given id
         */
        List<QuestionItem> listAuctionQuestions(String auctionId, PagingWindow window) throws ServiceException;

        /**
         * Lists all auctions created by the user with the given id.
         * 
         * @param username the id of the user to list auctions for
         * @param open     whether to list only open auctions
         * @return the auctions created by the user with the given id
         */
        List<AuctionItem> listUserAuctions(String username, boolean open) throws ServiceException;

        /**
         * Lists all auctions that are currently open and the user with the given id has
         * placed a bid on.
         * 
         * @param username the id of the user to list auctions for
         * @return the auctions that are currently open and the user with the given id
         *         has
         */
        List<AuctionItem> listAuctionsFollowedByUser(String username) throws ServiceException;

        /**
         * Lists a number of auctions that are close to ending.
         * 
         * @return the auctions that are close to ending
         */
        List<AuctionItem> listAuctionsAboutToClose() throws ServiceException;

        /**
         * Lists a number of auctions that were recently created.
         * 
         * @return the auctions that were recently created
         */
        List<AuctionItem> listRecentAuctions() throws ServiceException;

        /**
         * Lists a number of auctions that are currently popular.
         * 
         * @return the auctions that are currently popular
         */
        List<AuctionItem> listPopularAuctions() throws ServiceException;
}
