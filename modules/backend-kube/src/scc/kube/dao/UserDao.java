package scc.kube.dao;

import java.time.LocalDateTime;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

public class UserDao {
    public static enum Status {
        ACTIVE, INACTIVE
    }

    public ObjectId id;
    public String username;
    public String name;

    @BsonProperty(value = "hashed_password")
    public String hashedPassword;

    @BsonProperty(value = "profile_image_id")
    public String profileImageId;

    public Status status;

    @BsonProperty(value = "create_time")
    public LocalDateTime createTime;

    public UserDao() {
    }
}