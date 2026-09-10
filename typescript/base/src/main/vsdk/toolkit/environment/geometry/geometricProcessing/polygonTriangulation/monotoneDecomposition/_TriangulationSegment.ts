import { Vector2Dd } from "../../../../../common/linealAlgebra/Vector2Dd.js";
import type { _TriangulationTrapezoidQueryNode } from "./_TriangulationTrapezoidQueryNode.js";
export class _TriangulationSegment {
    public startPoint = new Vector2Dd();
    public endPoint = new Vector2Dd();
    public hasBeenInserted = false;
    public startPointQueryNode: _TriangulationTrapezoidQueryNode | null = null;
    public endPointQueryNode: _TriangulationTrapezoidQueryNode | null = null;
    public nextSegmentIndex = 0;
    public previousSegmentIndex = 0;
}
