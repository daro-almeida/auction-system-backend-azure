package scc.data;

import scc.utils.Hash;

import java.util.Arrays;

/**
 * Represents a User, as stored in the database
 */
public class UserDAO {
    private String _rid;
    private String _ts;
    private String id;
    private String nickname;
    private String name;
    private String hashedPwd;
    private String photoId;

    public UserDAO(User u) {
        this(u.getId(), u.getNickname(), u.getName(), u.getHashedPwd(), u.getPhotoId());
    }

    public UserDAO(String id, String nickname, String name, String hashedPwd, String photoId) {
        super();
        this.id = id;
        this.nickname = nickname;
        this.name = name;
        this.hashedPwd = hashedPwd;
        this.photoId = photoId;
    }

    public String get_rid() {
        return _rid;
    }

    public void set_rid(String _rid) {
        this._rid = _rid;
    }

    public String get_ts() {
        return _ts;
    }

    public void set_ts(String _ts) {
        this._ts = _ts;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public User toUser() {
        return new User(id, nickname, name, hashedPwd, photoId);
    }

    @Override
    public String toString() {
        return "UserDAO{" +
                "_rid='" + _rid + '\'' +
                ", _ts='" + _ts + '\'' +
                ", id='" + id + '\'' +
                ", nickname='" + nickname + '\'' +
                ", name='" + name + '\'' +
                ", hashedPwd='" + hashedPwd + '\'' +
                ", photoId='" + photoId + '\'' +
                '}';
    }
}
