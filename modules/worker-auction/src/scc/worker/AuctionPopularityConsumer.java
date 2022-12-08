package scc.worker;

import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import redis.clients.jedis.Jedis;
import scc.kube.Kube;
import scc.kube.KubeSerde;
import scc.kube.Rabbitmq;
import scc.kube.Redis;
import scc.kube.config.KubeEnv;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

public class AuctionPopularityConsumer {

    public static void main(String[] args) throws IOException, TimeoutException {
        var config = KubeEnv.getKubeConfig();

        var jedis = Kube.createJedis(config.getRedisConfig());
        var connection = Rabbitmq.createConnectionFromConfig(config.getRabbitmqConfig());
        var channel = connection.createChannel();
        Rabbitmq.declareBroadcastBidsExchange(channel);
        var queue = channel.queueDeclare("", false, true, true, null).getQueue();
        channel.queueBind(queue, Rabbitmq.EXCHANGE_BROADCAST_BIDS, "");
        channel.basicConsume(queue, true, new BidCallback(jedis), consumerTag -> {
        });
    }

    // Increment the popularity of an auction every time a bid is made
    static class BidCallback implements DeliverCallback {
        private static final Logger logger = Logger.getLogger(BidCallback.class.getName());

        private final Jedis jedis;

        public BidCallback(Jedis jedis) {
            this.jedis = jedis;
        }

        @Override
        public void handle(String consumerTag, Delivery message) throws IOException {
            try {
                var createdBid = KubeSerde.fromJson(message.getBody(), Rabbitmq.CreatedBid.class);
                Redis.incrementPopularAuction(this.jedis, createdBid.auctionId());

            } catch (Exception e) {
                e.printStackTrace();
                logger.severe("Failed to process bid: " + e.getMessage());
            }
        }
    }
}
