package scc.cache;

public interface QuestionCache {

    String QUESTION_PREFIX = "question:";

    void set(String questionId, String questionJson);
}
