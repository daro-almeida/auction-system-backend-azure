package scc.worker;

import scc.kube.Kube;
import scc.kube.Redis;
import scc.kube.config.KubeEnv;

public class AuctionPopularityUpdater {
    public static void main(String[] args) {
        var config = KubeEnv.getKubeConfig();

        var jedis = Kube.createJedis(config.getRedisConfig());
        Redis.updatePopularAuctions(jedis);
    }
}
