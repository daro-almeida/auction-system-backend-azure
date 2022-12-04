package scc.kube;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.bson.types.ObjectId;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import scc.kube.config.RabbitmqConfig;

public class Rabbitmq implements AutoCloseable {
    public static record CreatedBid(
            ObjectId auctionId,
            ObjectId bidId) {
    }

    public static record CreatedAuction(
            ObjectId auctionId) {
    }

    public static final String ROUTING_KEY_USER_DELETE = "user-delete";
    public static final String ROUTING_KEY_AUCTION_CLOSE = "auction-close";

    // Broadcast creation of bids
    public static final String EXCHANGE_BROADCAST_BIDS = "broadcast-bids";
    // Broadcast creation of auctions
    public static final String EXCHANGE_BROADCAST_AUCTIONS = "broadcast-auctions";

    private final Channel channel;
    private final boolean shouldClose;

    public Rabbitmq(Connection connection) throws IOException, TimeoutException {
        this.channel = createChannelFromConnection(connection);
        this.shouldClose = true;
    }

    public Rabbitmq(Channel channel) throws IOException {
        this.channel = channel;
        this.shouldClose = false;
    }

    /**
     * Queue the deletion of a user.
     * 
     * @param userId The hex-encoded user ID.
     */
    public void deleteUser(String userId) {
        try {
            channel.basicPublish("", ROUTING_KEY_USER_DELETE, null, userId.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Queue the closing of an auction.
     * 
     * @param auctionId The hex-encoded auction ID.
     */
    public void closeAuction(String auctionId) {
        try {
            channel.basicPublish("", ROUTING_KEY_AUCTION_CLOSE, null, auctionId.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void broadcastCreatedBid(ObjectId auctionId, ObjectId bidId) {
        try {
            var createdBid = new CreatedBid(auctionId, bidId);
            var messageContent = KubeSerde.toJson(createdBid);
            channel.basicPublish(EXCHANGE_BROADCAST_BIDS, "", null, messageContent.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void broadcastCreatedAuction(ObjectId auctionId) {
        try {
            var createdAuction = new CreatedAuction(auctionId);
            var messageContent = KubeSerde.toJson(createdAuction);
            channel.basicPublish(EXCHANGE_BROADCAST_AUCTIONS, "", null, messageContent.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        if (this.shouldClose)
            this.channel.close();
    }

    @WithSpan
    public static Connection createConnectionFromConfig(RabbitmqConfig config) throws IOException, TimeoutException {
        var factory = new ConnectionFactory();
        factory.setHost(config.host);
        factory.setPort(config.port);
        return factory.newConnection();
    }

    @WithSpan
    public static Channel createChannelFromConnection(Connection connection) throws IOException {
        var channel = connection.createChannel();
        declare(channel);
        return channel;
    }

    public static void declareUserDeleteQueue(Channel channel) throws IOException {
        channel.queueDeclare(ROUTING_KEY_USER_DELETE, true, false, false, null);
    }

    public static void declareAuctionCloseQueue(Channel channel) throws IOException {
        channel.queueDeclare(ROUTING_KEY_AUCTION_CLOSE, true, false, false, null);
    }

    public static void declareBroadcastBidsExchange(Channel channel) throws IOException {
        channel.exchangeDeclare(EXCHANGE_BROADCAST_BIDS, "fanout");
    }

    public static void declareBroadcastAuctionsExchange(Channel channel) throws IOException {
        channel.exchangeDeclare(EXCHANGE_BROADCAST_AUCTIONS, "fanout");
    }

    public static void declare(Channel channel) throws IOException {
        declareUserDeleteQueue(channel);
        declareAuctionCloseQueue(channel);
        declareBroadcastBidsExchange(channel);
        declareBroadcastAuctionsExchange(channel);
    }
}
