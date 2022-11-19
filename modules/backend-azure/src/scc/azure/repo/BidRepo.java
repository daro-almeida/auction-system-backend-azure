package scc.azure.repo;

import java.util.List;

import scc.Result;
import scc.ServiceError;
import scc.azure.dao.BidDAO;

public interface BidRepo {
    public Result<BidDAO, ServiceError> getBid(String id);

    public Result<BidDAO, ServiceError> insertBid(BidDAO bid);

    public Result<List<BidDAO>, ServiceError> listAuctionBids(String auctionId);
}
