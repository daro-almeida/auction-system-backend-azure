package scc.azure.repo;

import java.util.List;

import scc.Result;
import scc.ServiceError;
import scc.azure.dao.AuctionDAO;

public interface AuctionRepo {
    public Result<AuctionDAO, ServiceError> getAuction(String id);

    public Result<AuctionDAO, ServiceError> insertAuction(AuctionDAO auction);

    public Result<AuctionDAO, ServiceError> updateAuction(AuctionDAO auction);

    public Result<List<AuctionDAO>, ServiceError> listUserAuctions(String userId, boolean open);

    public Result<List<AuctionDAO>, ServiceError> listAuctionsFollowedByUser(String userId);

    public Result<List<AuctionDAO>, ServiceError> listAuctionsAboutToClose();

    public Result<List<AuctionDAO>, ServiceError> listRecentAuctions();

    public Result<List<AuctionDAO>, ServiceError> listPopularAuctions();

    public Result<List<AuctionDAO>, ServiceError> queryAuctions(String query);
}
