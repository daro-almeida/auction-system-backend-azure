package scc.kube;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import scc.kube.config.KubeEnv;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException, TimeoutException {
        var config = KubeEnv.getKubeConfig();
        var channel = Rabbitmq.createConnectionFromConfig(config.getRabbitmqConfig());
        var rabbitmq = new Rabbitmq(channel);
        rabbitmq.deleteUser(args[0]);
    }
}
