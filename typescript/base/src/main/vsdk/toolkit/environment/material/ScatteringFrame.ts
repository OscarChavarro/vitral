//= References:                                                             =
//= [ARVO1995b] Arvo, James. "Stratified Sampling of Spherical Triangles". =
//=     SIGGRAPH '95, 1995, pp. 437-438.                                    =
//= [GLAS1989] Glassner, Andrew S. (ed.). "An Introduction to Ray Tracing".=
//=     Academic Press, 1989.                                               =

import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";

/**
Double precision vector arithmetic of the Phong scattering model, written
out component by component in the order RenderPark's Vector3D uses, so that
directions, cosines and densities do not depend on Vector3Dd's methods.
*/
export namespace scatteringArithmetic {
    /** x * b.x + y * b.y + z * b.z, in that order */
    export function dot(a: Vector3Dd, b: Vector3Dd): number {
        return a.x() * b.x() + a.y() * b.y() + a.z() * b.z();
    }

    /** s * v */
    export function scale(s: number, v: Vector3Dd): Vector3Dd {
        return new Vector3Dd(s * v.x(), s * v.y(), s * v.z());
    }

    /** a * v + b * w */
    export function combine(a: number, v: Vector3Dd, b: number, w: Vector3Dd): Vector3Dd {
        return new Vector3Dd(a * v.x() + b * w.x(), a * v.y() + b * w.y(), a * v.z() + b * w.z());
    }

    /**
    v scaled by the inverse of its norm, left as it is when the norm is within
    epsilon of 0
    */
    export function normalize(v: Vector3Dd, epsilon: number): Vector3Dd {
        const n = Math.sqrt(v.x() * v.x() + v.y() * v.y() + v.z() * v.z());
        const factor = n < -epsilon || n > epsilon ? 1.0 / n : 1.0;
        return scale(factor, v);
    }
}

/**
Orthonormal frame around a unit Z axis, with the hemisphere sampling of the
Phong scattering model ([ARVO1995b] section 2: a square of variates mapped
to angles on the sphere, in a frame aligned with the axis of the lobe), and
the ideal reflection and refraction directions it is centered on.

Direction convention: `in` is the propagation direction of the arriving
ray, so it points towards the surface (the opposite of PhongLobe's `in`,
which leaves it), `normal` leaves the surface on the side the ray comes
from, a returned direction leaves the surface. Densities are per
unit solid angle. Variates are supplied by the caller, who draws them.

Immutable value; all arithmetic in double precision. C++ output pointers are one-element holders.
*/
export class ScatteringFrame {
    /** Smallest |sin| of the Z axis' polar angle that still defines X from it */
    public static readonly DEGENERATE_AXIS = 1e-6;

    /** Tolerance of the normalizations */
    public static readonly NORMALIZATION_EPSILON = 1e-6;

    private readonly x: Vector3Dd;
    private readonly y: Vector3Dd;
    private readonly z: Vector3Dd;

    /** [ARVO1995b] equation 6: [x|z] = Normalize(x - (x.z) z), in closed form */
    private static horizontalAxis(unitZ: Vector3Dd): Vector3Dd {
        const zz = Math.sqrt(1.0 - unitZ.z() * unitZ.z());
        if (zz < ScatteringFrame.DEGENERATE_AXIS) {
            return new Vector3Dd(1.0, 0.0, 0.0);
        }
        return new Vector3Dd(unitZ.y() / zz, -unitZ.x() / zz, 0.0);
    }

    /** Y = Z x X completes the frame */
    private static completingAxis(unitZ: Vector3Dd, unitX: Vector3Dd): Vector3Dd {
        return new Vector3Dd(
            unitZ.y() * unitX.z() - unitZ.z() * unitX.y(),
            unitZ.z() * unitX.x() - unitZ.x() * unitX.z(),
            unitZ.x() * unitX.y() - unitZ.y() * unitX.x(),
        );
    }

    /**
    Frame whose Z axis is the given unit vector; X is horizontal
    (z = 0) when Z is not vertical, (1, 0, 0) when it is, and Y = Z x X.
    [ARVO1995b] equation 6 gives X up to its sign.
    */
    public constructor(unitZ: Vector3Dd) {
        this.x = ScatteringFrame.horizontalAxis(unitZ);
        this.y = ScatteringFrame.completingAxis(unitZ, this.x);
        this.z = unitZ;
    }

    public getX(): Vector3Dd {
        return this.x;
    }

    public getY(): Vector3Dd {
        return this.y;
    }

    public getZ(): Vector3Dd {
        return this.z;
    }

    /**
    Direction distributed as cos(theta) around Z, from variates xi1 (azimuth)
    and xi2 (polar); its density cos(theta) / pi goes to density[0]
    */
    public sampleCosine(xi1: number, xi2: number, density: number[]): Vector3Dd {
        // [ARVO1995b] section 2: (xi1, xi2) in [0,1]^2 mapped to azimuth and polar angle
        const phi = 2.0 * Math.PI * xi1;
        const cosPhi = Math.cos(phi);
        const sinPhi = Math.sin(phi);
        const cosTheta = Math.sqrt(1.0 - xi2);
        const sinTheta = Math.sqrt(xi2);

        const planar = scatteringArithmetic.combine(cosPhi, this.x, sinPhi, this.y);
        density[0] = cosTheta / Math.PI;
        return scatteringArithmetic.combine(sinTheta, planar, cosTheta, this.z);
    }

    /**
    Direction distributed as cos(theta)^n around Z; its density
    (n + 1) cos(theta)^n / (2 pi), computed in double precision, goes to
    density[0]
    */
    public samplePhong(n: number, xi1: number, xi2: number, density: number[]): Vector3Dd {
        // [ARVO1995b] section 2: the same mapping with the radial distribution of cos^n
        const phi = 2.0 * Math.PI * xi1;
        const cosPhi = Math.cos(phi);
        const sinPhi = Math.sin(phi);
        const cosTheta = Math.pow(xi2, 1.0 / (n + 1));
        const sinTheta = Math.sqrt(1.0 - cosTheta * cosTheta);

        const planar = scatteringArithmetic.combine(cosPhi, this.x, sinPhi, this.y);
        density[0] = ((n + 1.0) * Math.pow(cosTheta, n)) / (2.0 * Math.PI);
        return scatteringArithmetic.combine(sinTheta, planar, cosTheta, this.z);
    }

    /** Mirror direction of `in` about `normal`, normalized */
    public static idealReflection(inDirection: Vector3Dd, normal: Vector3Dd): Vector3Dd {
        // The product is evaluated in single precision, then widened
        const twice = 2 * scatteringArithmetic.dot(normal, inDirection);
        const along = scatteringArithmetic.scale(twice, normal);
        const reflected = new Vector3Dd(
            inDirection.x() - along.x(),
            inDirection.y() - along.y(),
            inDirection.z() - along.z(),
        );
        return scatteringArithmetic.normalize(reflected, ScatteringFrame.NORMALIZATION_EPSILON);
    }

    /**
    Refracted direction of `in` through the interface whose normal is
    `normal` ([GLAS1989]), with the real parts of the refraction indices on
    the side of `in` and beyond; on total internal reflection
    totalInternalReflection[0] is set and the mirror direction is returned
    */
    public static idealRefraction(
        inDirection: Vector3Dd,
        normal: Vector3Dd,
        inIndex: number,
        outIndex: number,
        totalInternalReflection: boolean[],
    ): Vector3Dd {
        // [GLAS1989]: real part of the indices only
        const ratio = inIndex / outIndex;
        const cosIncident = -scatteringArithmetic.dot(inDirection, normal);
        const cosTransmitted2 = 1 + ratio * ratio * (cosIncident * cosIncident - 1);
        if (cosTransmitted2 < 0) {
            totalInternalReflection[0] = true;
            return ScatteringFrame.idealReflection(inDirection, normal);
        }
        totalInternalReflection[0] = false;
        const cosTransmitted = Math.sqrt(cosTransmitted2);
        const normalScale = ratio * cosIncident - cosTransmitted;
        const scaled = scatteringArithmetic.scale(ratio, inDirection);
        const refracted = new Vector3Dd(
            scaled.x() + normalScale * normal.x(),
            scaled.y() + normalScale * normal.y(),
            scaled.z() + normalScale * normal.z(),
        );
        return scatteringArithmetic.normalize(refracted, ScatteringFrame.NORMALIZATION_EPSILON);
    }
}
