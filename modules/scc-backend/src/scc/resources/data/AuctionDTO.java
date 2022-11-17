package scc.resources.data;

import scc.services.data.AuctionItem;

public class AuctionDTO {
    public String title;
    public String description;
    public String userId;
    public String imageId;

    public AuctionDTO() {
    }

    public AuctionDTO(String title, String description, String userId, String imageId) {
        this.title = title;
        this.description = description;
        this.userId = userId;
        this.imageId = imageId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public static AuctionDTO from(AuctionItem item){
        return new AuctionDTO(item.getTitle(), item.getDescription(), item.getUserId(), item.getPictureId());
    }

    @Override
    public String toString() {
        return "AuctionDTO [title=" + title + ", description=" + description + ", userId=" + userId + ", imageId="
                + imageId + "]";
    }
}
