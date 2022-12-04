package scc.worker;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import redis.clients.jedis.Jedis;
import scc.kube.Kube;
import scc.kube.Mongo;
import scc.kube.Rabbitmq;
import scc.kube.config.KubeEnv;

/**
 * Worker that replaces the "*_display" fields so that they display "deleted"
 * instead of the users name.
 * This assumes the user is already deactivated.
 */
public class UserScrub {

    public static void main(String[] args) throws IOException, TimeoutException {
        var rabbitConfig = KubeEnv.getRabbitmqConfig();
        var mongoConfig = KubeEnv.getMongoConfig();
        var redisConfig = KubeEnv.getRedisConfig();
        var connection = Rabbitmq.createConnectionFromConfig(rabbitConfig);
        var channel = connection.createChannel();
        var mongo = new Mongo(mongoConfig);
        var jedis = Kube.createJedis(redisConfig);

        channel.basicQos(1);

        var deleteUserCallback = new DeleteUserCallback(channel, mongo, jedis);
        Rabbitmq.declareUserDeleteQueue(channel);
        channel.basicConsume(Rabbitmq.ROUTING_KEY_USER_DELETE, false, deleteUserCallback, consumerTag -> {
        });
    }
}

class DeleteUserCallback implements DeliverCallback {
    private final Channel channel;
    private final Mongo mongo;
    private final Jedis jedis;

    public DeleteUserCallback(Channel channel, Mongo mongo, Jedis jedis) {
        this.channel = channel;
        this.mongo = mongo;
        this.jedis = jedis;
    }

    @Override
    public void handle(String consumerTag, Delivery message) throws IOException {
        System.out.println("Scrubbing user " + new String(message.getBody()));
    }
}