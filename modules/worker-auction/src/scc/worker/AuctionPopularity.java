package scc.worker;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import redis.clients.jedis.Jedis;
import scc.AppLogic;
import scc.kube.Kube;
import scc.kube.KubeSerde;
import scc.kube.Rabbitmq;
import scc.kube.Redis;
import scc.kube.config.KubeEnv;

public class AuctionPopularity {
    public static void main(String[] args) throws InterruptedException, IOException, TimeoutException {
        var config = KubeEnv.getKubeConfig();

        { // Setup period task
            var jedis = Kube.createJedis(config.getRedisConfig());
            var task = new PeriodicPopularityUpdate(jedis);
            new Thread(task).start();
        }

        { // Setup consumer
            var jedis = Kube.createJedis(config.getRedisConfig());
            var connection = Rabbitmq.createConnectionFromConfig(config.getRabbitmqConfig());
            var channel = connection.createChannel();
            Rabbitmq.declareBroadcastBidsExchange(channel);
            var queue = channel.queueDeclare("", false, true, true, null).getQueue();
            channel.queueBind(queue, Rabbitmq.EXCHANGE_BROADCAST_BIDS, "");
            channel.basicConsume(queue, true, new BidCallback(channel, jedis), consumerTag -> {
            });
        }
    }
}

// Increment the popularity of an auction every time a bid is made
class BidCallback implements DeliverCallback {
    private static final Logger logger = Logger.getLogger(BidCallback.class.getName());

    private final Channel channel;
    private final Jedis jedis;

    public BidCallback(Channel channel, Jedis jedis) {
        this.channel = channel;
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

// Periodically update the popularity of all auctions and reset the popularity
// counters
class PeriodicPopularityUpdate implements Runnable {
    private final Jedis jedis;

    public PeriodicPopularityUpdate(Jedis jedis) {
        this.jedis = jedis;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(AppLogic.DURATION_UPDATE_AUCTION_POPULARITY.toMillis());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Redis.updatePopularAuctions(this.jedis);
        }
    }
}