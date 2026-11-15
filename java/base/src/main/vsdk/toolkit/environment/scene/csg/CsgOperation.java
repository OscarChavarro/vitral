package vsdk.toolkit.environment.scene.csg;

/** Values preserve the existing POV operation identifiers. */
public enum CsgOperation {
    UNION(9), INTERSECTION(10), DIFFERENCE(11);

    private final int value;

    CsgOperation(int value) { this.value = value; }

    /** The cpp enumerator value. */
    public int value() { return value; }
}
