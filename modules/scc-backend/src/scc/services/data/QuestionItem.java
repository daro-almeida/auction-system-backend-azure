package scc.services.data;

import scc.azure.dao.QuestionDAO;

import java.util.Optional;

public class QuestionItem {
    private final String id;
    private final String question;
    private final String authorId;
    private final Optional<ReplyItem> reply;

    public QuestionItem(String id, String question, String authorId, ReplyItem reply) {
        this.id = id;
        this.question = question;
        this.authorId = authorId;
        this.reply = Optional.of(reply);
    }

    public QuestionItem(String id, String question, String authorId) {
        this.id = id;
        this.question = question;
        this.authorId = authorId;
        this.reply = Optional.empty();
    }

    public static QuestionItem fromQuestionDAO(QuestionDAO questionDAO) {
        return new QuestionItem(questionDAO.getId(), questionDAO.getQuestion(), questionDAO.getUserId(),
                new ReplyItem(questionDAO.getReply().getUserId(), questionDAO.getReply().getReply()));
    }

    public String getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public String getAuthorId() {
        return authorId;
    }

    public Optional<ReplyItem> getReply() {
        return reply;
    }

    @Override
    public String toString() {
        return "QuestionItem [id=" + id + ", question=" + question + ", authorId=" + authorId + ", reply=" + reply
                + "]";
    }
}
