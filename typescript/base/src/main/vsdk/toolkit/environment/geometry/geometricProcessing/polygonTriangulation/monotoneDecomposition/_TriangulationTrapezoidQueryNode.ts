import { Vector2Dd } from "../../../../../common/linealAlgebra/Vector2Dd.js";
import { _Construct } from "./_Construct.js";
export class _TriangulationTrapezoidQueryNode {
    public queryNodeType = 0;
    public segmentIndex = 0;
    public splitPoint = new Vector2Dd();
    public trapezoidIndex = 0;
    public parent: _TriangulationTrapezoidQueryNode | null = null;
    public leftChild: _TriangulationTrapezoidQueryNode | null = null;
    public rightChild: _TriangulationTrapezoidQueryNode | null = null;
    public locateEndpoint(p: Vector2Dd, other: Vector2Dd): number {
        if (this.queryNodeType === _Construct.T_SINK) return this.trapezoidIndex;
        if (this.queryNodeType === _Construct.T_Y) {
            if (_Construct.greaterThan(p, this.splitPoint)) return this.rightChild!.locateEndpoint(p, other);
            return (
                _Construct.equalTo(p, this.splitPoint) && _Construct.greaterThan(other, this.splitPoint)
                    ? this.rightChild
                    : this.leftChild
            )!.locateEndpoint(p, other);
        }
        if (this.queryNodeType === _Construct.T_X) {
            const s = _Construct.segmentAt(this.segmentIndex);
            if (_Construct.equalTo(p, s.startPoint) || _Construct.equalTo(p, s.endPoint)) {
                if (_Construct.fpEqual(p.y, other.y))
                    return (other.x < p.x ? this.leftChild : this.rightChild)!.locateEndpoint(p, other);
                return (
                    _TriangulationTrapezoidQueryNode.isLeftOf(this.segmentIndex, other)
                        ? this.leftChild
                        : this.rightChild
                )!.locateEndpoint(p, other);
            }
            return (
                _TriangulationTrapezoidQueryNode.isLeftOf(this.segmentIndex, p) ? this.leftChild : this.rightChild
            )!.locateEndpoint(p, other);
        }
        return 0;
    }
    private static isLeftOf(segmentIndex: number, p: Vector2Dd): boolean {
        const s = _Construct.segmentAt(segmentIndex);
        const [a, b] = _Construct.greaterThan(s.endPoint, s.startPoint)
            ? [s.startPoint, s.endPoint]
            : [s.endPoint, s.startPoint];
        const area = _Construct.fpEqual(b.y, p.y)
            ? p.x < b.x
                ? 1
                : -1
            : _Construct.fpEqual(a.y, p.y)
              ? p.x < a.x
                  ? 1
                  : -1
              : _Construct.cross(a, b, p);
        return area > 0;
    }
}
