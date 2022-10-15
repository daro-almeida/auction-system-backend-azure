package scc.utils;

import java.util.function.Function;

/**
 * 
 * Represents the result of an operation, either wrapping a result of the given
 * type,
 * or an error.
 * 
 * @param <T> type of the result value associated with success
 * @param <E> type of the result value associated with failure
 */
public interface Result<T, E> {

    /**
     * Tests if the result is Ok.
     */
    boolean isOk();

    /**
     * Tests if the result is Err.
     */
    boolean isErr();

    /**
     * obtains the payload value of this result
     * 
     * @return the value of this result.
     */
    T value();

    /**
     * obtains the payload value of this result
     * 
     * @return the value of this result.
     */
    T unwrap();

    /**
     *
     * obtains the error code of this result
     * 
     * @return the error code
     * 
     */
    E unwrapErr();

    <F> Result<F, E> andThen(Function<T, Result<F, E>> fn);

    <F> Result<F, E> map(Function<T, F> fn);

    /**
     * Convenience method for returning non error results of the given type
     * 
     * @param Class of value of the result
     * @return the value of the result
     */
    static <T, E> Result<T, E> ok(T result) {
        return new OkResult<>(result);
    }

    /**
     * Convenience method for returning non error results without a value
     * 
     * @return non-error result
     */
    static <T, E> OkResult<T, E> ok() {
        return new OkResult<>(null);
    }

    /**
     * Convenience method used to return an error
     * 
     * @return
     */
    static <T, E> ErrorResult<T, E> error(E error) {
        return new ErrorResult<>(error);
    }

    static <T, E> Result<T, E> flatten(Result<Result<T, E>, E> result) {
        if (result.isErr())
            return Result.error(result.unwrapErr());
        return result.value();
    }
}

/*
 * 
 */
class OkResult<T, E> implements Result<T, E> {

    final T result;

    OkResult(T result) {
        this.result = result;
    }

    @Override
    public boolean isOk() {
        return true;
    }

    @Override
    public T value() {
        return result;
    }

    @Override
    public E unwrapErr() {
        throw new IllegalStateException("Cannot unwrap error from Ok result");
    }

    public String toString() {
        return "(OK, " + value() + ")";
    }

    @Override
    public boolean isErr() {
        return false;
    }

    @Override
    public <F> Result<F, E> andThen(Function<T, Result<F, E>> fn) {
        return fn.apply(this.result);
    }

    @Override
    public <F> Result<F, E> map(Function<T, F> fn) {
        return Result.ok(fn.apply(this.result));
    }

    @Override
    public T unwrap() {
        return this.value();
    }
}

class ErrorResult<T, E> implements Result<T, E> {

    final E error;

    ErrorResult(E error) {
        this.error = error;
    }

    @Override
    public boolean isOk() {
        return false;
    }

    @Override
    public T value() {
        throw new RuntimeException("Attempting to extract the value of an Error: " + error);
    }

    @Override
    public E unwrapErr() {
        return error;
    }

    public String toString() {
        return "(" + error + ")";
    }

    @Override
    public boolean isErr() {
        return true;
    }

    @Override
    public <F> Result<F, E> andThen(Function<T, Result<F, E>> fn) {
        return Result.error(this.error);
    }

    @Override
    public <F> Result<F, E> map(Function<T, F> fn) {
        return Result.error(this.error);
    }

    @Override
    public T unwrap() {
        return this.value();
    }
}