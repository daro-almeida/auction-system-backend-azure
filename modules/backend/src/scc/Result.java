package scc;

import java.util.Optional;
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
    boolean isError();

    /**
     * obtains the payload value of this result
     * 
     * @return the value of this result.
     */
    T value();

    /**
     *
     * obtains the error code of this result
     * 
     * @return the error code
     * 
     */
    E error();

    /**
     * Get the value of this result it exists.
     */
    Optional<T> toOk();

    /**
     * Get the error of this result it exists.
     */
    Optional<E> toErr();

    /**
     * obtains the error message of this result
     * 
     * @return
     */
    String errorMessage();

    <F> Result<F, E> andThen(Function<T, Result<F, E>> fn);

    /**
     * Maps a Result<T, E> to Result<U, E> by applying a function to a contained
     * Ok value, leaving an Err value untouched.
     * 
     * @param <U>    the type of the value contained in the result
     * @param mapper the function to apply to the value
     * @return the result of the function
     */
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

    static <T, E, S> ErrorResult<T, E> err(Result<S, E> result) {
        return err(result.error(), result.errorMessage());
    }

    /**
     * Convenience method used to return an error
     * 
     * @return
     */
    static <T, E> ErrorResult<T, E> err(E error) {
        return new ErrorResult<>(error);
    }

    /**
     * Convenience method used to return an error
     * 
     * @return
     */
    static <T, E> ErrorResult<T, E> err(E error, String message) {
        return new ErrorResult<>(error, message);
    }

    static <T, E> Result<T, E> flatten(Result<Result<T, E>, E> result) {
        if (result.isError())
            return Result.err(result.error());
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
    public E error() {
        throw new IllegalStateException("Cannot unwrap error from Ok result");
    }

    @Override
    public Optional<T> toOk() {
        return Optional.ofNullable(result);
    }

    @Override
    public Optional<E> toErr() {
        return Optional.empty();
    }

    @Override
    public String errorMessage() {
        throw new IllegalStateException("Cannot unwrap error from Ok result");
    }

    public String toString() {
        return "(OK, " + value() + ")";
    }

    @Override
    public boolean isError() {
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

}

class ErrorResult<T, E> implements Result<T, E> {

    final E error;
    final String message;

    ErrorResult(E error) {
        this.error = error;
        this.message = null;
    }

    ErrorResult(E error, String message) {
        this.error = error;
        this.message = message;
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
    public E error() {
        return error;
    }

    @Override
    public Optional<T> toOk() {
        return Optional.empty();
    }

    @Override
    public Optional<E> toErr() {
        return Optional.ofNullable(error);
    }

    @Override
    public String errorMessage() {
        return message;
    }

    public String toString() {
        return "(" + error + ": " + message + ")";
    }

    @Override
    public boolean isError() {
        return true;
    }

    @Override
    public <F> Result<F, E> andThen(Function<T, Result<F, E>> fn) {
        return Result.err(this.error);
    }

    @Override
    public <F> Result<F, E> map(Function<T, F> fn) {
        return Result.err(this.error);
    }

}