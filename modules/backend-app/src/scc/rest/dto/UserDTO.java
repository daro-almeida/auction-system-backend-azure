package scc.rest.dto;

import scc.item.UserItem;
import scc.rest.ResourceUtils;

public class UserDTO {

    public String id;
    public String name;
    public String photoId;

    public UserDTO() {
    }

    public UserDTO(String id, String name, String photoId) {
        this.id = id;
        this.name = name;
        this.photoId = photoId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public static UserDTO from(UserItem userItem) {
        return new UserDTO(
                userItem.getId(),
                userItem.getName(),
                userItem.getPhotoId().map(ResourceUtils::mediaIdToString).orElse(null));
    }

    @Override
    public String toString() {
        return "UserDTO [id=" + id + ", name=" + name + ", photoId=" + photoId + "]";
    }
}
