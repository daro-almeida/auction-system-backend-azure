package scc.worker;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TreeMap;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.types.ObjectId;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import scc.kube.Kube;
import scc.kube.KubeData;
import scc.kube.Mongo;
import scc.kube.Rabbitmq;
import scc.kube.config.KubeEnv;

public class AuctionClose {
    private static final Logger logger = Logger.getLogger(AuctionClose.class.getName());

    public static void main(String[] args) throws IOException, TimeoutException {
        Logger rootLog = Logger.getLogger("");
        rootLog.setLevel(Level.INFO);
        rootLog.getHandlers()[0].setLevel(Level.INFO);

        var kubeConfig = KubeEnv.getKubeConfig();
        var rabbitConfig = KubeEnv.getRabbitmqConfig();
        var mongoConfig = KubeEnv.getMongoConfig();
        var redisConfig = KubeEnv.getRedisConfig();
        var connection = Rabbitmq.createConnectionFromConfig(rabbitConfig);

        var mongo = new Mongo(mongoConfig);
        var jedis = Kube.createJedis(redisConfig);
        var data = new KubeData(kubeConfig, mongo, jedis);

        {
            logger.info("Spawning auction close worker");
            var channel = connection.createChannel();
            var rabbitmq = new Rabbitmq(connection, channel);
            new Thread(new CloseAuctionWorker(mongo, rabbitmq)).start();
        }

        {
            logger.info("Spawning auction close consumer");
            var channel = connection.createChannel();
            var closeAuctionCallback = new CloseAuctionCallback(channel, data);
            Rabbitmq.declareAuctionCloseQueue(channel);
            channel.basicQos(1);
            channel.basicConsume(Rabbitmq.ROUTING_KEY_AUCTION_CLOSE, false, closeAuctionCallback, consumerTag -> {
            });
        }
    }
}

class CloseAuctionCallback implements DeliverCallback {
    private static final Logger logger = Logger.getLogger(CloseAuctionCallback.class.getName());

    private final Channel channel;
    private final KubeData data;

    public CloseAuctionCallback(Channel channel, KubeData data) {
        this.channel = channel;
        this.data = data;
    }

    @Override
    public void handle(String consumerTag, Delivery message) throws IOException {
        try {
            var auctionId = new ObjectId(new String(message.getBody()));
            logger.info("Closing auction " + auctionId);

            var result = data.closeAuction(auctionId);
            logger.info("Closed auction " + auctionId + " with result " + result);

            channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("Closing auction failed");
        }
    }

}

// Periodically check for auctions that are about to close and queue them for
// closing.
class CloseAuctionWorker implements Runnable {
    private static final Logger logger = Logger.getLogger(CloseAuctionWorker.class.getName());

    // How often to lookup for auctions that are about to close
    private static final Duration LOOKUP_INTERVAL = Duration.ofSeconds(2);
    // How long before an auction closes to queue it in the worker
    private static final Duration CLOSE_THRESHOLD = Duration.ofMinutes(5);

    private final Mongo mongo;
    private final Rabbitmq rabbitmq;

    public CloseAuctionWorker(Mongo mongo, Rabbitmq rabbitmq) {
        this.mongo = mongo;
        this.rabbitmq = rabbitmq;
    }

    @Override
    public void run() {
        var nextLookup = Instant.now();
        var pending = new TreeMap<ObjectId, Instant>();
        while (true) {
            // Close auctions
            while (!pending.isEmpty() && pending.firstEntry().getValue().isBefore(Instant.now())) {
                var entry = pending.pollFirstEntry();
                var auctionId = entry.getKey();
                logger.info("Queueing auction " + auctionId);
                this.rabbitmq.closeAuction(auctionId.toHexString());
            }

            // Find auctions that are about to close
            if (Instant.now().isAfter(nextLookup)) {
                var closingBefore = LocalDateTime.now().plus(CLOSE_THRESHOLD);
                var soonToClose = this.mongo.getAuctionSoonToClose(closingBefore);
                for (var auction : soonToClose) {
                    var ttc = Duration.between(LocalDateTime.now(), auction.closeTime);
                    pending.put(auction.id, Instant.now().plus(ttc));
                    logger.info("Found auction " + auction.id + " closing in " + ttc);
                }
                nextLookup = Instant.now().plus(LOOKUP_INTERVAL);
            }

            { // Setup next sleep
                var now = Instant.now();
                var durToNextLookup = Duration.between(now, nextLookup);
                var durToNextClose = pending.isEmpty() ? durToNextLookup
                        : Duration.between(now, pending.firstEntry().getValue());
                var minDuration = durToNextLookup.compareTo(durToNextClose) < 0 ? durToNextLookup : durToNextClose;
                sleep(minDuration);
            }
        }

    }

    private static void sleep(Duration duration) {
        try {
            var millis = duration.toMillis();
            if (millis < 0)
                return;
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}