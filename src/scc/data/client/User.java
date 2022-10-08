package scc.data.client;

import scc.utils.Hash;

/**
 * Represents a User, as returned to the clients
 */
public class User {
    private final String id;
    private String nickname;
    private String name;
    private String hashedPwd;
    private String photoId;

    public User(String nickname, String name, String pwd, String photoId) {
        this(generateUserID(), nickname, name, Hash.of(pwd), photoId);
    }

    public User(String id, String nickname, String name, String hashedPwd, String photoId) {
        this.id = id;
        this.nickname = nickname;
        this.name = name;
        this.hashedPwd = hashedPwd;
        this.photoId = photoId;
    }

    private static String generateUserID() {
        return String.valueOf(System.currentTimeMillis());
    }

    public String getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHashedPwd() {
        return hashedPwd;
    }

    public void setPwd(String pwd) {
        this.hashedPwd = Hash.of(pwd);
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", nickname='" + nickname + '\'' +
                ", name='" + name + '\'' +
                ", pwd='" + hashedPwd + '\'' +
                ", photoId='" + photoId + '\'' +
                '}';
    }
}
