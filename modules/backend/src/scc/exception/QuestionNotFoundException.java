package scc.exception;

public class QuestionNotFoundException extends ServiceException {
    public QuestionNotFoundException() {
        super("Question not found");
    }
}
