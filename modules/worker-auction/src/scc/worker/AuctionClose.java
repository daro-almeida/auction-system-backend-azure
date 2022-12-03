package scc.worker;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
import scc.kube.KubeSerde;
import scc.kube.Mongo;
import scc.kube.Rabbitmq;
import scc.kube.config.KubeEnv;

public class AuctionClose {
    private static final Logger logger = Logger.getLogger(AuctionClose.class.getName());

    // How often to lookup for auctions that are about to close
    static final Duration LOOKUP_INTERVAL = Duration.ofMinutes(2);
    // How long before an auction closes to queue it in the worker
    static final Duration CLOSE_THRESHOLD = Duration.ofMinutes(5);

    public static void main(String[] args) {
        try {
            Logger rootLog = Logger.getLogger("");
            rootLog.setLevel(Level.INFO);
            rootLog.getHandlers()[0].setLevel(Level.INFO);
            run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void run() throws IOException, TimeoutException {
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
            logger.info("Spawning auction create consumer");
            var channel = connection.createChannel();
            Rabbitmq.declareBroadcastAuctionsExchange(channel);
            var rabbitmq = new Rabbitmq(connection, connection.createChannel());
            var queue = channel.queueDeclare(Rabbitmq.EXCHANGE_BROADCAST_AUCTIONS, false, true, true, null).getQueue();
            channel.queueBind(queue, Rabbitmq.EXCHANGE_BROADCAST_AUCTIONS, "");
            var callback = new CloseAuctionWatcher(mongo, rabbitmq);
            channel.basicQos(1);
            channel.basicConsume(queue, true, callback, consumerTag -> {
            });
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
                var closingBefore = LocalDateTime.now().plus(AuctionClose.CLOSE_THRESHOLD);
                var soonToClose = this.mongo.getAuctionSoonToClose(closingBefore);
                for (var auction : soonToClose) {
                    var ttc = Duration.between(LocalDateTime.now(), auction.closeTime);
                    pending.put(auction.id, Instant.now().plus(ttc));
                    logger.info("Found auction " + auction.id + " closing in " + ttc);
                }
                nextLookup = Instant.now().plus(AuctionClose.LOOKUP_INTERVAL);
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

// Watch the created auctions exchange and queue auctions with short duration
// times.
class CloseAuctionWatcher implements DeliverCallback {
    private static final Logger logger = Logger.getLogger(CloseAuctionWatcher.class.getName());

    private final Mongo mongo;
    private final Rabbitmq rabbitmq;

    public CloseAuctionWatcher(Mongo mongo, Rabbitmq rabbitmq) {
        this.mongo = mongo;
        this.rabbitmq = rabbitmq;
    }

    @Override
    public void handle(String consumerTag, Delivery message) throws IOException {
        try {
            var createdAuction = KubeSerde.fromJson(message.getBody(), Rabbitmq.CreatedAuction.class);
            handleCreated(createdAuction.auctionId());
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("Failed to handle created auction");
        }
    }

    private void handleCreated(ObjectId auctionId) {
        var auctionDao = this.mongo.getAuction(auctionId).value();
        var ttc = Duration.between(LocalDateTime.now(ZoneOffset.UTC), auctionDao.closeTime);
        if (ttc.compareTo(AuctionClose.CLOSE_THRESHOLD) < 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (ttc.toMillis() > 0)
                            Thread.sleep(ttc.toMillis());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    logger.info("Queueing auction " + auctionDao.id);
                    rabbitmq.closeAuction(auctionDao.id.toHexString());
                }
            }).start();
        }
    }

}