package vsdk.toolkit.environment.geometry.surface;

/**
Legacy last-hit index; queries carrying their own index must not depend on it.
cpp stores it in a java::AtomicLong; the Java port keeps its established
per-thread storage (ThreadLocal) behind the same get/set interface.
*/
public class TriangleMeshIntersectionIndex {
    private final ThreadLocal<Integer> value = ThreadLocal.withInitial(() -> -1);

    public TriangleMeshIntersectionIndex() {}
    public TriangleMeshIntersectionIndex(TriangleMeshIntersectionIndex other) { value.set(other.get()); }
    public TriangleMeshIntersectionIndex assign(TriangleMeshIntersectionIndex other)
    {
        if (this != other) value.set(other.get());
        return this;
    }
    public int get() { return value.get(); }
    public void set(int index) { value.set(index); }
}
