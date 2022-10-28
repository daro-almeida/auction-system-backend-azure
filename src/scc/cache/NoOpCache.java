package scc.cache;

public class NoOpCache implements Cache {

    @Override
    public String get(String key) {
        return null;
    }

    @Override
    public String set(String key, String value) {
        return null;
    }

    @Override
    public Long expire(String key, int seconds) {
        return 0L;
    }

    @Override
    public Long del(String... keys) {
        return 0L;
    }

    @Override
    public Long deleteAuction(String auctionId) {
        return 0L;
    }

    @Override
    public Long deleteQuestion(String questionId) {
        return 0L;
    }
}
