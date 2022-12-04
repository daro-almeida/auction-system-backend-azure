package scc.kube;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Connection;

import redis.clients.jedis.JedisPool;
import scc.AuctionService;
import scc.MediaService;
import scc.ServiceFactory;
import scc.UserService;
import scc.kube.config.KubeEnv;

public class KubeServices {
    private final JedisPool jedisPool;
    private final Mongo mongo;
    private final Connection rabbitmqConnection;

    public KubeServices() throws IOException, TimeoutException {
        var config = KubeEnv.getKubeConfig();
        this.jedisPool = Kube.createJedisPool(config.getRedisConfig());
        this.mongo = new Mongo(config.getMongoConfig());
        this.rabbitmqConnection = Rabbitmq.createConnectionFromConfig(config.getRabbitmqConfig());
    }

    public ServiceFactory<UserService> getUserServiceFactory() {
        return new KubeUserServiceFactory(this.jedisPool, this.mongo);
    }

    public ServiceFactory<AuctionService> getAuctionServiceFactory() {
        return new KubeAuctionServiceFactory(this.jedisPool, this.mongo, this.rabbitmqConnection);
    }

    public ServiceFactory<MediaService> getMediaServiceFactory() {
        return new KubeMediaServiceFactory(KubeEnv.getKubeMediaConfig());
    }
}
