package scc.kube;

import java.util.List;

import org.apache.commons.pool2.ObjectPool;

import com.rabbitmq.client.Channel;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import redis.clients.jedis.JedisPool;
import scc.AuctionService;
import scc.ServiceFactory;
import scc.utils.AuctionServiceWithResources;
import scc.utils.AutoCloseableFn;

public class KubeAuctionServiceFactory implements ServiceFactory<AuctionService> {

    private final JedisPool jedisPool;
    private final Mongo mongo;
    private final ObjectPool<Channel> rabbitmqPool;

    public KubeAuctionServiceFactory(JedisPool jedisPool, Mongo mongo, ObjectPool<Channel> rabbitmqConnection) {
        this.jedisPool = jedisPool;
        this.mongo = mongo;
        this.rabbitmqPool = rabbitmqConnection;
    }

    @Override
    @WithSpan
    public AuctionService createService() {
        try {
            var jedis = jedisPool.getResource();
            var auth = new RedisAuth(jedis, mongo);
            var repo = new KubeRepo(jedis, mongo);
            var channel = this.rabbitmqPool.borrowObject();
            var channelResource = new AutoCloseableFn(() -> this.rabbitmqPool.returnObject(channel));
            assert channel.isOpen();
            var rabbitmq = new Rabbitmq(channel);
            var service = new KubeAuctionService(auth, repo, rabbitmq);
            return new AuctionServiceWithResources(service, List.of(jedis, rabbitmq, channelResource));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create auction service", e);
        }
    }

}
