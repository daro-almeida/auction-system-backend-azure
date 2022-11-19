package scc.azure.dao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a User, as stored in the database
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDAO {
    public static enum Status {
        ACTIVE,
        DELETED
    }

    private String _rid;
    private String _ts;
    private String id;
    private String name;
    private String hashedPwd;
    private String photoId;
    private Status status;

    public UserDAO(String id, String name, String hashedPwd, String photoId, Status status) {
        this.id = id;
        this.name = name;
        this.hashedPwd = hashedPwd;
        this.photoId = photoId;
        this.status = status;
    }

    public UserDAO() {
    }

    public String get_rid() {
        return _rid;
    }

    public String get_ts() {
        return _ts;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHashedPwd() {
        return hashedPwd;
    }

    public String getPhotoId() {
        return photoId;
    }

    public Status getStatus() {
        return status;
    }

    public void set_rid(String _rid) {
        this._rid = _rid;
    }

    public void set_ts(String _ts) {
        this._ts = _ts;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHashedPwd(String hashedPwd) {
        this.hashedPwd = hashedPwd;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "UserDAO [_rid=" + _rid + ", _ts=" + _ts + ", id=" + id + ", name=" + name + ", hashedPwd=" + hashedPwd
                + ", photoId=" + photoId + ", status=" + status + "]";
    }

}
