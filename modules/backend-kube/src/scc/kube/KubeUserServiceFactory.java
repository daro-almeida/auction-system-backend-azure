package scc.kube;

import java.util.List;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import redis.clients.jedis.JedisPool;
import scc.ServiceFactory;
import scc.UserService;
import scc.utils.UserServiceWithResources;

public class KubeUserServiceFactory implements ServiceFactory<UserService> {

    private final JedisPool jedisPool;
    private final Mongo mongo;

    public KubeUserServiceFactory(JedisPool jedisPool, Mongo mongo) {
        this.jedisPool = jedisPool;
        this.mongo = mongo;
    }

    @Override
    @WithSpan
    public UserService createService() {
        var jedis = this.jedisPool.getResource();
        var auth = new RedisAuth(jedis, mongo);
        var repo = new KubeRepo(jedis, mongo);
        var service = new KubeUserService(auth, repo);
        return new UserServiceWithResources(service, List.of(jedis));
    }

}
