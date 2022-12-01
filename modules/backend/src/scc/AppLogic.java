package scc;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Application Logic common to a
 */
public class AppLogic {
    /**
     * The maximum number of auctions that can be returned by the operation
     * that retreives the most recent auctions.
     */
    public static final int MAX_RECENT_AUCTIONS = 20;

    /**
     * The maximum number of auctions that can be returned by the operation
     * that retreives the most popular auctions.
     */
    public static final int MAX_MOST_POPULAR_AUCTIONS = 20;

    /**
     * The maximum number of auctions that can be returned by the operation
     * that retreives the soon to close auctions.
     */
    public static final int MAX_ABOUT_TO_CLOSE_AUCTIONS = 20;

    /**
     * Duration befora an auction is closed that it is considered to be about
     * to close.
     */
    public static final Duration DURATION_ABOUT_TO_CLOSE_THRESHOLD = Duration.ofMinutes(5);

    /**
     * Interval at which the auction popularity list is updated.
     * The most popular auctions are updated with the most popular auctions from the
     * list time window.
     */
    public static final Duration DURATION_UPDATE_AUCTION_POPULARITY = Duration.ofMinutes(1);

    /**
     * The maximum number of auctions that can be returned by a search query.
     */
    public static final int MAX_AUCTION_QUERY_RESULTS = 20;

    /**
     * The maximum number of questions that can be returned by a search query.
     */
    public static final int MAX_QUESTION_QUERY_RESULTS = 20;

    /**
     * The reserved user id and name for the deleted user.
     */
    public static final String DELETED_USER_ID = "deleted";
    public static final String DELETED_USER_NAME = DELETED_USER_ID;

    public static boolean isAuctionEndTimeAboutToClose(LocalDateTime now, LocalDateTime auctionEndTime) {
        return now.plus(DURATION_ABOUT_TO_CLOSE_THRESHOLD).isAfter(auctionEndTime);
    }

    public static boolean isAuctionEndTimeAboutToClose(LocalDateTime auctionEndTime) {
        return isAuctionEndTimeAboutToClose(LocalDateTime.now(), auctionEndTime);
    }
}
