package scc.kube;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.rabbitmq.client.Channel;
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
    private final ObjectPool<Channel> channelPool;

    public KubeServices() throws IOException, TimeoutException {
        var config = KubeEnv.getKubeConfig();
        this.jedisPool = Kube.createJedisPool(config.getRedisConfig());
        this.mongo = new Mongo(config.getMongoConfig());
        this.rabbitmqConnection = Rabbitmq.createConnectionFromConfig(config.getRabbitmqConfig());

        var poolConfig = new GenericObjectPoolConfig<Channel>();
        poolConfig.setMinIdle(16);
        poolConfig.setMaxTotal(128);
        this.channelPool = new GenericObjectPool<>(new PooledObjectFactory<Channel>() {

            @Override
            public void activateObject(PooledObject<Channel> p) throws Exception {
            }

            @Override
            public void destroyObject(PooledObject<Channel> p) throws Exception {
                p.getObject().close();
            }

            @Override
            public PooledObject<Channel> makeObject() throws Exception {
                return new DefaultPooledObject<Channel>(Rabbitmq.createChannelFromConnection(rabbitmqConnection));
            }

            @Override
            public void passivateObject(PooledObject<Channel> p) throws Exception {
            }

            @Override
            public boolean validateObject(PooledObject<Channel> p) {
                return p.getObject().isOpen();
            }

        }, poolConfig);
    }

    public ServiceFactory<UserService> getUserServiceFactory() {
        return new KubeUserServiceFactory(this.jedisPool, this.mongo);
    }

    public ServiceFactory<AuctionService> getAuctionServiceFactory() {
        return new KubeAuctionServiceFactory(this.jedisPool, this.mongo, this.channelPool);
    }

    public ServiceFactory<MediaService> getMediaServiceFactory() {
        return new KubeMediaServiceFactory(KubeEnv.getKubeMediaConfig());
    }
}
