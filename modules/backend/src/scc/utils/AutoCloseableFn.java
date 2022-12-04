package scc.utils;

public class AutoCloseableFn implements AutoCloseable {

    public interface Fn {
        void run() throws Exception;
    }

    private final Fn func;

    public AutoCloseableFn(Fn func) {
        this.func = func;
    }

    @Override
    public void close() throws Exception {
        this.func.run();
    }

}
