import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";

/**
Direction of a ray crossing the interface between two media, by Snell's
law n1 sin(theta1) = n2 sin(theta2), in the vector form used by Whitted
[WHIT1980] and in GLSL `refract`.  Both input vectors are unit vectors on
the same side of the interface: `towardViewer` points back along the
incoming ray and `normal` points into the medium the ray comes from.
*/
export class SnellRefraction {
    /**
    Writes the transmitted direction for relative index eta = n1 / n2 and
    returns true, or returns false when the angle exceeds the critical
    angle (total internal reflection) and leaves `outDirection` untouched.
    `outDirection` is a one-element output holder.
    */
    public static refract(towardViewer: Vector3Dd, normal: Vector3Dd, eta: number, outDirection: Vector3Dd[]): boolean {
        const cosI: number = towardViewer.dotProduct(normal);
        const k: number = 1.0 - eta * eta * (1.0 - cosI * cosI);
        if (k < 0.0) {
            return false;
        }
        const c: number = eta * cosI - Math.sqrt(k);
        outDirection[0] = new Vector3Dd(
            -eta * towardViewer.x() + c * normal.x(),
            -eta * towardViewer.y() + c * normal.y(),
            -eta * towardViewer.z() + c * normal.z(),
        );
        return true;
    }

    /** Mirror direction of `towardViewer` about `normal`. */
    public static reflect(towardViewer: Vector3Dd, normal: Vector3Dd): Vector3Dd {
        const twoCos: number = 2.0 * towardViewer.dotProduct(normal);
        return new Vector3Dd(
            twoCos * normal.x() - towardViewer.x(),
            twoCos * normal.y() - towardViewer.y(),
            twoCos * normal.z() - towardViewer.z(),
        );
    }
}
