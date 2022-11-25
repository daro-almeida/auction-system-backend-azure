package scc.kube;

import java.io.IOException;
import scc.kube.config.KubeEnv;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        var config = KubeEnv.getMongoConfig();
        var mongo = new Mongo(config);

        mongo.close();
    }
}
