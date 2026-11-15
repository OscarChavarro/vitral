import { Double } from "../../../../java/lang/Double.js";
import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { AffinePlacementStatus } from "./AffinePlacementStatus.js";

// Pair in the column-vector convention. A valid pair maps geometry to its
// parent and back without changing affine ray parameters.
export class AffinePlacement {
    private forward: Matrix4x4d;
    private inverse: Matrix4x4d;
    private status: AffinePlacementStatus;

    public constructor();
    /** C++ copy constructor. */
    public constructor(other: AffinePlacement);
    public constructor(other?: AffinePlacement) {
        this.forward = other ? other.forward : new Matrix4x4d();
        this.inverse = other ? other.inverse : new Matrix4x4d();
        this.status = other ? other.status : AffinePlacementStatus.Valid;
    }

    public assign(value: Matrix4x4d, inverseValue: Matrix4x4d): AffinePlacementStatus {
        this.forward = value;
        this.inverse = inverseValue;
        this.status = AffinePlacement.validate(this.forward, this.inverse);
        return this.status;
    }

    public getForward(): Matrix4x4d {
        return this.forward;
    }
    public getInverse(): Matrix4x4d {
        return this.inverse;
    }
    public getStatus(): AffinePlacementStatus {
        return this.status;
    }

    public static validate(forward: Matrix4x4d, inverse: Matrix4x4d): AffinePlacementStatus {
        for (let row = 0; row < 4; ++row)
            for (let column = 0; column < 4; ++column) {
                if (!Double.isFinite(forward.get(row, column))) return AffinePlacementStatus.Invalid;
                if (!Double.isFinite(inverse.get(row, column))) return AffinePlacementStatus.Indeterminate;
            }
        if (
            forward.get(3, 0) !== 0 ||
            forward.get(3, 1) !== 0 ||
            forward.get(3, 2) !== 0 ||
            forward.get(3, 3) !== 1 ||
            inverse.get(3, 0) !== 0 ||
            inverse.get(3, 1) !== 0 ||
            inverse.get(3, 2) !== 0 ||
            inverse.get(3, 3) !== 1
        )
            return AffinePlacementStatus.Invalid;
        const a = forward.get(0, 0),
            b = forward.get(0, 1),
            c = forward.get(0, 2);
        const d = forward.get(1, 0),
            e = forward.get(1, 1),
            f = forward.get(1, 2);
        const g = forward.get(2, 0),
            h = forward.get(2, 1),
            i = forward.get(2, 2);
        const determinant = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
        if (!Double.isFinite(determinant)) return AffinePlacementStatus.Indeterminate;
        if (determinant === 0) {
            // A visibly zero row or column proves singularity. Otherwise the
            // determinant may have underflowed or suffered cancellation.
            for (let k = 0; k < 3; ++k) {
                if (
                    (forward.get(k, 0) === 0 && forward.get(k, 1) === 0 && forward.get(k, 2) === 0) ||
                    (forward.get(0, k) === 0 && forward.get(1, k) === 0 && forward.get(2, k) === 0)
                )
                    return AffinePlacementStatus.Invalid;
            }
            return AffinePlacementStatus.Indeterminate;
        }
        const products = [forward.multiply(inverse), inverse.multiply(forward)];
        for (let k = 0; k < 2; ++k)
            for (let row = 0; row < 4; ++row)
                for (let column = 0; column < 4; ++column) {
                    const actual = products[k]!.get(row, column),
                        expected = row === column ? 1.0 : 0.0;
                    if (!Double.isFinite(actual) || JavaMath.abs(actual - expected) > 1e-8)
                        return AffinePlacementStatus.Indeterminate;
                }
        return AffinePlacementStatus.Valid;
    }
}
