package scc.exception;

public class QuestionAlreadyRepliedException extends ServiceException {
    public QuestionAlreadyRepliedException() {
        super("Question already replied");
    }
}
