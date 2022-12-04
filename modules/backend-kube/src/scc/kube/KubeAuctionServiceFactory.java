package scc.kube;

import java.io.IOException;
import java.util.List;

import com.rabbitmq.client.Connection;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import redis.clients.jedis.JedisPool;
import scc.AuctionService;
import scc.ServiceFactory;
import scc.utils.AuctionServiceWithResources;

public class KubeAuctionServiceFactory implements ServiceFactory<AuctionService> {

    private final JedisPool jedisPool;
    private final Mongo mongo;
    private final Connection rabbitmqConnection;

    public KubeAuctionServiceFactory(JedisPool jedisPool, Mongo mongo, Connection rabbitmqConnection) {
        this.jedisPool = jedisPool;
        this.mongo = mongo;
        this.rabbitmqConnection = rabbitmqConnection;
    }

    @Override
    @WithSpan
    public AuctionService createService() {
        try {
            var jedis = jedisPool.getResource();
            var auth = new RedisAuth(jedis, mongo);
            var repo = new KubeRepo(jedis, mongo);
            var rabbitmq = new Rabbitmq(this.rabbitmqConnection);
            var service = new KubeAuctionService(auth, repo, rabbitmq);
            return new AuctionServiceWithResources(service, List.of(jedis, rabbitmq));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create auction service", e);
        }
    }

}
