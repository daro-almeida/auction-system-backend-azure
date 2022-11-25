package scc.kube;

import java.io.IOException;

import redis.clients.jedis.JedisPool;
import scc.kube.config.KubeConfig;
import scc.kube.config.KubeMediaConfig;

public class KubeServiceFactory {
    private Mongo mongo;
    private JedisPool jedisPool;

    public KubeServiceFactory() {
        this.mongo = null;
        this.jedisPool = null;
    }

    public KubeMediaService createMediaService(KubeMediaConfig mediaConfig) throws IOException {
        return new KubeMediaService(mediaConfig);
    }

    public KubeUserService createUserService(KubeConfig config) {
        if (this.mongo == null)
            this.mongo = new Mongo(config.getMongoConfig());

        if (this.jedisPool == null)
            this.jedisPool = Kube.createJedisPool(config.getRedisConfig());

        return new KubeUserService(config, this.jedisPool, this.mongo);
    }
}
