import { _IndexedVertex } from "./_IndexedVertex.js";
export class _ContourData {
    public readonly vertices: _IndexedVertex[] = [];
    public readonly childContours: number[] = [];
    public signedArea = 0;
    public parentContour = -1;
    public depth = -1;
}
