// Indexes into the input leaf batch and that leaf's boundary array.
export class CsgRayEventReference {
    public leafIndex: number;
    public boundaryIndex: number;

    public constructor();
    public constructor(leaf: number, boundary: number);
    /** C++ copy constructor. */
    public constructor(other: CsgRayEventReference);
    public constructor(leaf?: number | CsgRayEventReference, boundary?: number) {
        if (leaf instanceof CsgRayEventReference) {
            this.leafIndex = leaf.leafIndex;
            this.boundaryIndex = leaf.boundaryIndex;
        } else {
            this.leafIndex = leaf ?? -1;
            this.boundaryIndex = boundary ?? -1;
        }
    }
}
