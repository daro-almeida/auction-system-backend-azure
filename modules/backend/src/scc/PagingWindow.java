package scc;

// TODO: replace skip,limit in methods with this class
public class PagingWindow {
    public final int skip;
    public final int limit;

    public PagingWindow(int skip, int limit) {
        this.skip = skip;
        this.limit = limit;
    }

    public PagingWindow() {
        this(0, Integer.MAX_VALUE);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + skip;
        result = prime * result + limit;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PagingWindow other = (PagingWindow) obj;
        if (skip != other.skip)
            return false;
        if (limit != other.limit)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PagingWindow [skip=" + skip + ", limit=" + limit + "]";
    }
}
