package scc.kube;

import java.io.IOException;

import redis.clients.jedis.JedisPool;
import scc.kube.config.KubeConfig;
import scc.kube.config.KubeMediaConfig;
import scc.kube.config.MongoConfig;
import scc.kube.config.RabbitmqConfig;
import scc.kube.config.RedisConfig;

public class KubeServiceFactory {
    private JedisPool jedisPool;
    private Mongo mongo;
    private Rabbitmq rabbitmq;

    public KubeServiceFactory() {
        this.jedisPool = null;
        this.mongo = null;
        this.rabbitmq = null;
    }

    public KubeMediaService createMediaService(KubeMediaConfig mediaConfig) throws IOException {
        return new KubeMediaService(mediaConfig);
    }

    public KubeUserService createUserService(KubeConfig config) {
        var jedisPool = this.getJedisPool(config.getRedisConfig());
        var mongo = this.getMongo(config.getMongoConfig());
        var rabbitmq = this.getRabbitmq(config.getRabbitmqConfig());
        return new KubeUserService(config, jedisPool, mongo, rabbitmq);
    }

    public KubeAuctionService createAuctionService(KubeConfig config) {
        var jedisPool = this.getJedisPool(config.getRedisConfig());
        var mongo = this.getMongo(config.getMongoConfig());
        var rabbitmq = this.getRabbitmq(config.getRabbitmqConfig());
        return new KubeAuctionService(config, jedisPool, mongo, rabbitmq);
    }

    private JedisPool getJedisPool(RedisConfig config) {
        if (this.jedisPool == null)
            this.jedisPool = Kube.createJedisPool(config);
        return this.jedisPool;
    }

    private Mongo getMongo(MongoConfig config) {
        if (this.mongo == null)
            this.mongo = new Mongo(config);
        return this.mongo;
    }

    private Rabbitmq getRabbitmq(RabbitmqConfig config) {
        try {
            if (this.rabbitmq == null)
                this.rabbitmq = new Rabbitmq(config);
            return this.rabbitmq;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create rabbitmq", e);
        }
    }
}
