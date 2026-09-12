import { Vector3Dd } from "../../../../../common/linealAlgebra/Vector3Dd.js";

export class _PolyhedralBoundedSolidProfileDifferenceFallbackSpec {
    public constructor(
        public readonly clippedProfileAtCut: Vector3Dd[],
        public readonly xCut: number,
        public readonly xMax: number,
        public readonly minuendBounds: number[],
    ) {}
}
