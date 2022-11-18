package scc;

public class MediaId {
    private final MediaNamespace namespace;
    private final String id;

    public MediaId(MediaNamespace namespace, String id) {
        this.namespace = namespace;
        this.id = id;
    }

    public MediaNamespace getNamespace() {
        return namespace;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "MediaId [namespace=" + namespace + ", id=" + id + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
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
        MediaId other = (MediaId) obj;
        if (namespace != other.namespace)
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
