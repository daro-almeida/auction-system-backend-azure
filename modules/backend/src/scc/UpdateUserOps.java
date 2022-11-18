package scc;

public class UpdateUserOps {
    private String name;
    private String password;
    private MediaId imageId;

    public UpdateUserOps() {
    }

    public boolean shouldUpdateName() {
        return this.name != null;
    }

    public UpdateUserOps updateName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public boolean shouldUpdatePassword() {
        return this.password != null;
    }

    public UpdateUserOps updatePassword(String password) {
        this.password = password;
        return this;
    }

    public String getPassword() {
        return this.password;
    }

    public boolean shouldUpdateImage() {
        return this.imageId != null;
    }

    public UpdateUserOps updateImage(MediaId imageId) {
        this.imageId = imageId;
        return this;
    }

    public MediaId getImageId() {
        if (this.imageId == null)
            throw new IllegalStateException("imageId is not set");
        return this.imageId;
    }
}
