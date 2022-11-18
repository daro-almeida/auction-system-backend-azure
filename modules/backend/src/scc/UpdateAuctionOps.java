package scc;

public class UpdateAuctionOps {
    private String title;
    private String description;
    private MediaId imageId;

    public UpdateAuctionOps() {
    }

    public boolean shouldUpdateTitle() {
        return this.title != null;
    }

    public UpdateAuctionOps updateTitle(String title) {
        this.title = title;
        return this;
    }

    public String getTitle() {
        return this.title;
    }

    public boolean shouldUpdateDescription() {
        return this.description != null;
    }

    public UpdateAuctionOps updateDescription(String description) {
        this.description = description;
        return this;
    }

    public String getDescription() {
        return this.description;
    }

    public boolean shouldUpdateImage() {
        return this.imageId != null;
    }

    public UpdateAuctionOps updateImage(MediaId imageId) {
        this.imageId = imageId;
        return this;
    }

    public MediaId getImage() {
        if (this.imageId == null)
            throw new IllegalStateException("image is not set");
        return this.imageId;
    }
}
