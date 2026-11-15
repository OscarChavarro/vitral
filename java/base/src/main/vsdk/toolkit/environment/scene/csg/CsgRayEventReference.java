package vsdk.toolkit.environment.scene.csg;



/**
Indexes into the input leaf batch and that leaf's boundary array.
*/
public class CsgRayEventReference {
    public long leafIndex;

    public long boundaryIndex;

    public CsgRayEventReference() {
        this.leafIndex = -1;
        this.boundaryIndex = -1;}

    public CsgRayEventReference(long leaf, long boundary) {
        this.leafIndex = leaf;
        this.boundaryIndex = boundary;}
}

