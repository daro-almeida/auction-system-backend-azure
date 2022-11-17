package scc.services.data;

public class ReplyItem {
    private final String authorId;
    private final String content;

    public ReplyItem(String authorId, String content) {
        this.authorId = authorId;
        this.content = content;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "ReplyItem [authorId=" + authorId + ", content=" + content + "]";
    }
}
