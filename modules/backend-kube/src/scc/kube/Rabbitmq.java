package scc.kube;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import scc.kube.config.RabbitmqConfig;

public class Rabbitmq implements AutoCloseable {
    public static final String QUEUE_USER_DELETE = "user-delete";

    private final Connection connection;
    private final Channel channel;

    public Rabbitmq(RabbitmqConfig config) throws IOException, TimeoutException {
        var factory = new ConnectionFactory();
        factory.setHost(config.host);
        factory.setPort(config.port);

        this.connection = factory.newConnection();
        this.channel = connection.createChannel();

        declareUserDeleteQueue(this.channel);
    }

    /**
     * Queue the deletion of a user.
     * 
     * @param userId The hex-encoded user ID.
     */
    public void deleteUser(String userId) {
        try {
            channel.basicPublish("", QUEUE_USER_DELETE, null, userId.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        this.channel.close();
        this.connection.close();
    }

    public static void declareUserDeleteQueue(Channel channel) throws IOException {
        channel.queueDeclare(QUEUE_USER_DELETE, true, false, false, null);
    }
}
