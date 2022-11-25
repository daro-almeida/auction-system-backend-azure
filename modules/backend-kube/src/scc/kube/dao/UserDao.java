package scc.kube.dao;

import org.bson.codecs.pojo.annotations.BsonProperty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserDao {
    public static enum Status {
        ACTIVE,
        INACTIVE
    }

    @JsonProperty("user_id")
    @BsonProperty(value = "user_id")
    private String userId;

    private String name;

    @JsonProperty("hashed_password")
    @BsonProperty(value = "hashed_password")
    private String hashedPassword;

    @JsonProperty("profile_picture_id")
    @BsonProperty(value = "profile_picture_id")
    private String profilePictureId;

    private Status status;

    public UserDao(String userId, String name, String hashedPassword, String profilePictureId, Status status) {
        this.userId = userId;
        this.name = name;
        this.hashedPassword = hashedPassword;
        this.profilePictureId = profilePictureId;
        this.status = status;
    }

    public UserDao() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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
        return "UserDao [userId=" + userId + ", name=" + name + ", hashedPassword=" + hashedPassword
                + ", profilePictureId=" + profilePictureId + ", status=" + status + "]";
    }

}