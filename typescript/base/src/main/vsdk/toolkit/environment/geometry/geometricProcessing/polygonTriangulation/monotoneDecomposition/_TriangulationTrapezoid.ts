import { Vector2Dd } from "../../../../../common/linealAlgebra/Vector2Dd.js";
import { _Construct } from "./_Construct.js";
import type { _TriangulationTrapezoidQueryNode } from "./_TriangulationTrapezoidQueryNode.js";
export class _TriangulationTrapezoid {
    public leftSegmentIndex = 0;
    public rightSegmentIndex = 0;
    public upperPoint = new Vector2Dd();
    public lowerPoint = new Vector2Dd();
    public upperLeftTrapezoidIndex = 0;
    public upperRightTrapezoidIndex = 0;
    public lowerLeftTrapezoidIndex = 0;
    public lowerRightTrapezoidIndex = 0;
    public sinkNode: _TriangulationTrapezoidQueryNode | null = null;
    public savedUpperNeighborIndex = 0;
    public savedUpperNeighborSide = 0;
    public status = _Construct.ST_VALID;
    public copyFrom(x: _TriangulationTrapezoid): void {
        Object.assign(this, x);
        this.upperPoint = new Vector2Dd(x.upperPoint);
        this.lowerPoint = new Vector2Dd(x.lowerPoint);
    }
    public insidePolygon(): number {
        if (this.status === _Construct.ST_INVALID || this.leftSegmentIndex <= 0 || this.rightSegmentIndex <= 0)
            return 0;
        if (
            (this.upperLeftTrapezoidIndex <= 0 && this.upperRightTrapezoidIndex <= 0) ||
            (this.lowerLeftTrapezoidIndex <= 0 && this.lowerRightTrapezoidIndex <= 0)
        )
            return _Construct.greaterThan(
                _Construct.segmentAt(this.rightSegmentIndex).endPoint,
                _Construct.segmentAt(this.rightSegmentIndex).startPoint,
            )
                ? 1
                : 0;
        return 0;
    }
}
