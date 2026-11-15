import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";

// Node of the bounding sphere tree of an adaptively subdivided Bezier patch:
// a leaf holds the four corners of a flat piece, an interior node the indices
// of its two or four children in the tree's node list.
export class SubdividedBezierPatchNode {
    public leaf: boolean;
    public center: Vector3Dd;
    public radiusSquared: number;
    public corners: Vector3Dd[] = [new Vector3Dd(), new Vector3Dd(), new Vector3Dd(), new Vector3Dd()];
    public children: number[] = [-1, -1, -1, -1];
    public childCount: number;

    public constructor();
    /** cpp implicit copy constructor. */
    public constructor(other: SubdividedBezierPatchNode);
    public constructor(other?: SubdividedBezierPatchNode) {
        if (other !== undefined) {
            this.leaf = other.leaf;
            this.center = other.center;
            this.radiusSquared = other.radiusSquared;
            this.childCount = other.childCount;
            for (let i = 0; i < 4; i++) this.corners[i] = other.corners[i]!;
            for (let i = 0; i < 4; i++) this.children[i] = other.children[i]!;
            return;
        }
        this.leaf = true;
        this.center = new Vector3Dd();
        this.radiusSquared = 0;
        this.childCount = 0;
    }
}
