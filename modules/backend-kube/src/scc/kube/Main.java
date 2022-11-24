package scc.kube;

import java.time.LocalDateTime;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoClients;

import scc.kube.config.MongoConfig;
import scc.kube.dao.AuctionDao;
import scc.kube.dao.BidDao;
import scc.kube.dao.UserDao;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        var config = new MongoConfig(
                "mongodb://localhost:27017",
                "scc-backend",
                "auctions",
                "bids",
                "questions",
                "users");
        var mongo = new Mongo(config);

        var auction = new AuctionDao();
        auction.setStatus(AuctionDao.Status.OPEN);

        var bid1 = new BidDao();
        bid1.setValue(10.0);
        var bid2 = new BidDao();
        bid2.setValue(20.0);

        mongo.createAuction(auction);
        mongo.createBid(bid1);
        mongo.createBid(bid2);

        mongo.updateHighestBid(auction, bid1);
        System.out.println("After update 1: " + auction);

        mongo.updateHighestBid(auction, bid2);
        System.out.println("After update 2: " + auction);

        mongo.close();

        // var pojoCodecRegistry =
        // CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build());
        // var codecRegistry =
        // CodecRegistries.fromRegistries(com.mongodb.MongoClientSettings.getDefaultCodecRegistry(),
        // pojoCodecRegistry);
        // var clientSettings = com.mongodb.MongoClientSettings.builder()
        // .codecRegistry(codecRegistry)
        // .build();
        // var client = MongoClients.create(clientSettings);
        // var database = client.getDatabase("scc-backend");
        // var collection = database.getCollection("users", UserDao.class);
        // var bcollection = database.getCollection("bids", BidDao.class);

        // var user = new UserDao();
        // user.setName("test name");
        // user.setStatus(UserDao.Status.ACTIVE);
        // collection.insertOne(user);

        // var bid = new BidDao();
        // bid.setTime(LocalDateTime.now());
        // bcollection.insertOne(bid);
        // System.out.println("Inserted bid: " + bid);

        // collection.find().forEach((UserDao u) -> System.out.println(u));
        // bcollection.find().forEach((BidDao u) -> System.out.println(u));

        // client.close();
    }
}
