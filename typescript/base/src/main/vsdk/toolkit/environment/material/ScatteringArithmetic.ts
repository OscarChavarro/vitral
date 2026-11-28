import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";

/**
Double precision vector arithmetic of the Phong scattering model, written
out component by component in the order RenderPark's Vector3D uses, so that
directions, cosines and densities do not depend on Vector3Dd's methods.
*/
export class ScatteringArithmetic {
    private constructor() {}
    /** x * b.x + y * b.y + z * b.z, in that order */
    public static dot(a: Vector3Dd, b: Vector3Dd): number {
        return a.x() * b.x() + a.y() * b.y() + a.z() * b.z();
    }

    /** s * v */
    public static scale(s: number, v: Vector3Dd): Vector3Dd {
        return new Vector3Dd(s * v.x(), s * v.y(), s * v.z());
    }

    /** a * v + b * w */
    public static combine(a: number, v: Vector3Dd, b: number, w: Vector3Dd): Vector3Dd {
        return new Vector3Dd(a * v.x() + b * w.x(), a * v.y() + b * w.y(), a * v.z() + b * w.z());
    }

    /**
    v scaled by the inverse of its norm, left as it is when the norm is within
    epsilon of 0
    */
    public static normalize(v: Vector3Dd, epsilon: number): Vector3Dd {
        const n = Math.sqrt(v.x() * v.x() + v.y() * v.y() + v.z() * v.z());
        const factor = n < -epsilon || n > epsilon ? 1.0 / n : 1.0;
        return ScatteringArithmetic.scale(factor, v);
    }
}
