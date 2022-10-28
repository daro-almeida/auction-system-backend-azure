package scc.cache;

public interface Cache {

    String USER_PREFIX = "user:";
    String AUCTION_PREFIX = "auction:";
    String BID_PREFIX = "bid:";
    String QUESTION_PREFIX = "question:";

    String get(String key);

    String set(String key, String value);

    Long expire(String key, int seconds);

    Long del(String... keys);

    Long deleteAuction(String auctionId);

    Long deleteQuestion(String questionId);

    //TODO more cache methods for different objects
}
