package scc.kube;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.bson.types.ObjectId;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import scc.kube.config.RabbitmqConfig;

public class Rabbitmq implements AutoCloseable {
    public static record CreatedBid(
            ObjectId auctionId,
            ObjectId bidId) {
    }

    public static final String ROUTING_KEY_USER_DELETE = "user-delete";
    public static final String ROUTING_KEY_AUCTION_CLOSE = "auction-close";
    public static final String EXCHANGE_BROADCAST_BIDS = "broadcast-bids";

    private final Connection connection;
    private final Channel channel;

    public Rabbitmq(RabbitmqConfig config) throws IOException, TimeoutException {
        this.connection = createConnectionFromConfig(config);
        this.channel = connection.createChannel();
        declare(this.channel);
    }

    public Rabbitmq(Connection connection, Channel channel) throws IOException {
        this.connection = connection;
        this.channel = channel;
        declare(this.channel);
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

    @Override
    public void close() throws Exception {
        this.channel.close();
        this.connection.close();
    }

    public static Connection createConnectionFromConfig(RabbitmqConfig config) throws IOException, TimeoutException {
        var factory = new ConnectionFactory();
        factory.setHost(config.host);
        factory.setPort(config.port);
        return factory.newConnection();
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

    private static void declare(Channel channel) throws IOException {
        declareUserDeleteQueue(channel);
        declareAuctionCloseQueue(channel);
        declareBroadcastBidsExchange(channel);
    }
}
