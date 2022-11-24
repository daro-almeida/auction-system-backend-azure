package scc.kube.dao;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

public class UserDao {
    public static enum Status {
        ACTIVE,
        INACTIVE
    }

    private ObjectId id;
    private String name;
    @BsonProperty(value = "hashed_password")
    private String hashedPassword;
    @BsonProperty(value = "profile_picture_id")
    private String profilePictureId;
    private Status status;

    public UserDao(ObjectId id, String name, String hashedPassword, String profilePictureId, Status status) {
        this.id = id;
        this.name = name;
        this.hashedPassword = hashedPassword;
        this.profilePictureId = profilePictureId;
        this.status = status;
    }

    public UserDao() {
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public String getProfilePictureId() {
        return profilePictureId;
    }

    public void setProfilePictureId(String profilePictureId) {
        this.profilePictureId = profilePictureId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "UserDao [id=" + id + ", name=" + name + ", hashedPassword=" + hashedPassword + ", profilePictureId="
                + profilePictureId + ", status=" + status + "]";
    }
}