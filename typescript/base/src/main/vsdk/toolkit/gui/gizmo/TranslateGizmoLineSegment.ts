import type { ColorRgb } from "../../common/color/ColorRgb.js";
import type { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";

/**
Java record: a straight line drawn by a `TranslateGizmo`, with its color. The
record accessors keep their Java names.
*/
export class TranslateGizmoLineSegment {
    public constructor(
        private readonly startPoint: Vector3Dd,
        private readonly endPoint: Vector3Dd,
        private readonly segmentColor: ColorRgb,
    ) {}

    public start(): Vector3Dd {
        return this.startPoint;
    }

    public end(): Vector3Dd {
        return this.endPoint;
    }

    public color(): ColorRgb {
        return this.segmentColor;
    }
}
