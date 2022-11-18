package scc.item;

import java.util.Optional;

import scc.MediaId;

public class UserItem {
    private String id;
    private String name;
    private Optional<MediaId> photoId;

    public UserItem(String id, String name, Optional<MediaId> photoId) {
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

    public Optional<MediaId> getPhotoId() {
        return photoId;
    }

    public void setPhotoId(Optional<MediaId> photoId) {
        this.photoId = photoId;
    }

    @Override
    public String toString() {
        return "UserItem [id=" + id + ", name=" + name + ", photoId=" + photoId + "]";
    }
}
