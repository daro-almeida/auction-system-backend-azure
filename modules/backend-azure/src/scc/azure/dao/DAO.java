package scc.azure.dao;

import java.util.Optional;

import scc.AuctionStatus;
import scc.azure.Azure;
import scc.item.AuctionItem;
import scc.item.BidItem;
import scc.item.QuestionItem;
import scc.item.ReplyItem;
import scc.item.UserItem;

public class DAO {

    public static AuctionItem auctionToItem(AuctionDAO auction, Optional<BidDAO> topBid) {
        return new AuctionItem(
                auction.getId(),
                auction.getTitle(),
                auction.getDescription(),
                auction.getUserId(),
                Azure.parseDateTime(auction.getEndTime()),
                Optional.ofNullable(auction.getPictureId()).map(Azure::mediaIdFromString),
                auction.getStartingPrice(),
                statusToAuctionStatus(auction.getStatus()),
                topBid.map(DAO::bidToItem));
    }

    public static BidItem bidToItem(BidDAO bid) {
        return new BidItem(
                bid.getId(),
                bid.getAuctionId(),
                bid.getUserId(),
                Azure.parseDateTime(bid.getTime()),
                bid.getAmount());
    }

    public static AuctionStatus statusToAuctionStatus(AuctionDAO.Status status) {
        return switch (status) {
            case OPEN -> AuctionStatus.OPEN;
            case CLOSED -> AuctionStatus.CLOSED;
            case DELETED -> throw new IllegalArgumentException("Cannot convert deleted status to auction status");
        };
    }

    public static QuestionItem questionToItem(QuestionDAO question) {
        return new QuestionItem(
                question.getId(),
                question.getAuctionId(),
                question.getUserId(),
                question.getQuestion(),
                Optional.ofNullable(question.getReply()).map(r -> DAO.replyToItem(question.getId(), r)));
    }

    public static ReplyItem replyToItem(String questionId, QuestionDAO.Reply reply) {
        return new ReplyItem(
                questionId,
                reply.getUserId(),
                reply.getReply());
    }

    public static UserItem userToItem(UserDAO user) {
        return new UserItem(
                user.getId(),
                user.getName(),
                Optional.ofNullable(user.getPhotoId()).map(Azure::mediaIdFromString));
    }
}
