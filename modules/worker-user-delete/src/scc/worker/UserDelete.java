package scc.worker;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import scc.kube.Rabbitmq;

class DeleteUserCallback implements DeliverCallback {
    private final Channel channel;

    public DeleteUserCallback(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void handle(String consumerTag, Delivery message) throws IOException {
        System.out.println("Deleting user " + new String(message.getBody()));
        this.channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
    }
}

public class UserDelete {

    public static void main(String[] args) throws IOException, TimeoutException {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        var connection = factory.newConnection();
        var channel = connection.createChannel();

        var deleteUserCallback = new DeleteUserCallback(channel);
        Rabbitmq.declareUserDeleteQueue(channel);
        channel.basicConsume(Rabbitmq.QUEUE_USER_DELETE, false, deleteUserCallback, consumerTag -> {
        });
    }
}
