package scc.services.data;

import scc.azure.dao.UserDAO;

import java.util.Optional;

public class UserItem {

    private final String id;
    private final String name;
    private final String hashedPwd;
    private final Optional<String> photoId;

    public UserItem(String id, String name, String hashedPwd, String photoId) {
        this.id = id;
        this.name = name;
        this.hashedPwd = hashedPwd;
        this.photoId = Optional.of(photoId);
    }

    public UserItem(String id, String name, String hashedPwd) {
        this.id = id;
        this.name = name;
        this.hashedPwd = hashedPwd;
        this.photoId = Optional.empty();
    }

    public static UserItem fromUserDAO(UserDAO userDAO) {
        return new UserItem(userDAO.getId(), userDAO.getName(), userDAO.getHashedPwd(), userDAO.getPhotoId());
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

    public Optional<String> getPhotoId() {
        return photoId;
    }
}
