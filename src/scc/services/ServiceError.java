package scc.services;

public enum ServiceError {
    BAD_REQUEST,
    INVALID_CREDENTIALS,

    USER_NOT_FOUND,
    USER_ALREADY_EXISTS,

    AUCTION_NOT_FOUND,
    QUESTION_NOT_FOUND,
    QUESTION_ALREADY_REPLIED,
}