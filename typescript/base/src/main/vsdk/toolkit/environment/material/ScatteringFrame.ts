//= References:                                                             =
//= [ARVO1995b] Arvo, James. "Stratified Sampling of Spherical Triangles". =
//=     SIGGRAPH '95, 1995, pp. 437-438.                                    =
//= [GLAS1989] Glassner, Andrew S. (ed.). "An Introduction to Ray Tracing".=
//=     Academic Press, 1989.                                               =

import { Vector3Df } from "../../common/linealAlgebra/Vector3Df.js";

const f = Math.fround;

/**
Single precision vector arithmetic of the Phong scattering model, written
out component by component in the order RenderPark's Vector3D uses, so that
directions, cosines and densities are bit-identical to RenderPark's. Every
`float` operation of the C++ code is rounded with `Math.fround` (an
operation on two floats evaluated in double precision and rounded once is
the IEEE single precision result).
*/
export namespace scatteringArithmetic {
    /** x * b.x + y * b.y + z * b.z, in that order */
    export function dot(a: Vector3Df, b: Vector3Df): number {
        return f(f(f(a.x() * b.x()) + f(a.y() * b.y())) + f(a.z() * b.z()));
    }

    /** s * v */
    export function scale(s: number, v: Vector3Df): Vector3Df {
        s = f(s);
        return new Vector3Df(f(s * v.x()), f(s * v.y()), f(s * v.z()));
    }

    /** a * v + b * w */
    export function combine(a: number, v: Vector3Df, b: number, w: Vector3Df): Vector3Df {
        a = f(a);
        b = f(b);
        return new Vector3Df(
            f(f(a * v.x()) + f(b * w.x())),
            f(f(a * v.y()) + f(b * w.y())),
            f(f(a * v.z()) + f(b * w.z())),
        );
    }

    /**
    v scaled by the inverse of its norm, left as it is when the norm is within
    epsilon of 0
    */
    export function normalize(v: Vector3Df, epsilon: number): Vector3Df {
        epsilon = f(epsilon);
        const n = f(Math.sqrt(f(f(f(v.x() * v.x()) + f(v.y() * v.y())) + f(v.z() * v.z()))));
        const factor = n < -epsilon || n > epsilon ? f(1.0 / n) : 1.0;
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

Immutable value; all arithmetic in single precision except where noted, as
RenderPark computes it. C++ output pointers are one-element holders.
*/
export class ScatteringFrame {
    /** Smallest |sin| of the Z axis' polar angle that still defines X from it */
    public static readonly DEGENERATE_AXIS = 1e-6;

    /** Tolerance of the normalizations */
    public static readonly NORMALIZATION_EPSILON = f(1e-6);

    private readonly xAxis: Vector3Df;
    private readonly yAxis: Vector3Df;
    private readonly zAxis: Vector3Df;

    /** [ARVO1995b] equation 6: [x|z] = Normalize(x - (x.z) z), in closed form */
    private static horizontalAxis(unitZ: Vector3Df): Vector3Df {
        const zz = f(Math.sqrt(f(1.0 - f(unitZ.z() * unitZ.z()))));
        if (zz < ScatteringFrame.DEGENERATE_AXIS) {
            return new Vector3Df(1.0, 0.0, 0.0);
        }
        return new Vector3Df(f(unitZ.y() / zz), f(-unitZ.x() / zz), 0.0);
    }

    /** Y = Z x X completes the frame */
    private static completingAxis(unitZ: Vector3Df, unitX: Vector3Df): Vector3Df {
        return new Vector3Df(
            f(f(unitZ.y() * unitX.z()) - f(unitZ.z() * unitX.y())),
            f(f(unitZ.z() * unitX.x()) - f(unitZ.x() * unitX.z())),
            f(f(unitZ.x() * unitX.y()) - f(unitZ.y() * unitX.x())),
        );
    }

    /**
    Frame whose Z axis is the given unit vector; X is horizontal
    (z = 0) when Z is not vertical, (1, 0, 0) when it is, and Y = Z x X.
    [ARVO1995b] equation 6 gives X up to its sign.
    */
    public constructor(unitZ: Vector3Df) {
        this.xAxis = ScatteringFrame.horizontalAxis(unitZ);
        this.yAxis = ScatteringFrame.completingAxis(unitZ, this.xAxis);
        this.zAxis = unitZ;
    }

    public getX(): Vector3Df {
        return this.xAxis;
    }

    public getY(): Vector3Df {
        return this.yAxis;
    }

    public getZ(): Vector3Df {
        return this.zAxis;
    }

    /**
    Direction distributed as cos(theta) around Z, from variates xi1 (azimuth)
    and xi2 (polar); its density cos(theta) / pi goes to density[0]
    */
    public sampleCosine(xi1: number, xi2: number, density: number[]): Vector3Df {
        // [ARVO1995b] section 2: (xi1, xi2) in [0,1]^2 mapped to azimuth and polar angle
        const phi = f(f(2.0 * f(Math.PI)) * f(xi1));
        const cosPhi = f(Math.cos(phi));
        const sinPhi = f(Math.sin(phi));
        const cosTheta = f(Math.sqrt(1.0 - xi2));
        const sinTheta = f(Math.sqrt(xi2));

        const planar = scatteringArithmetic.combine(cosPhi, this.xAxis, sinPhi, this.yAxis);
        density[0] = cosTheta / Math.PI;
        return scatteringArithmetic.combine(sinTheta, planar, cosTheta, this.zAxis);
    }

    /**
    Direction distributed as cos(theta)^n around Z; its density
    (n + 1) cos(theta)^n / (2 pi), computed in double precision, goes to
    density[0]
    */
    public samplePhong(n: number, xi1: number, xi2: number, density: number[]): Vector3Df {
        // [ARVO1995b] section 2: the same mapping with the radial distribution of cos^n
        const phi = f(f(2.0 * f(Math.PI)) * f(xi1));
        const cosPhi = f(Math.cos(phi));
        const sinPhi = f(Math.sin(phi));
        const cosTheta = f(Math.pow(xi2, 1.0 / (n + 1)));
        const sinTheta = f(Math.sqrt(1.0 - f(cosTheta * cosTheta)));

        const planar = scatteringArithmetic.combine(cosPhi, this.xAxis, sinPhi, this.yAxis);
        density[0] = ((n + 1.0) * Math.pow(cosTheta, n)) / (2.0 * Math.PI);
        return scatteringArithmetic.combine(sinTheta, planar, cosTheta, this.zAxis);
    }

    /** Mirror direction of `in` about `normal`, normalized */
    public static idealReflection(inDirection: Vector3Df, normal: Vector3Df): Vector3Df {
        // The product is evaluated in single precision, then widened
        const twice = f(2 * scatteringArithmetic.dot(normal, inDirection));
        const along = scatteringArithmetic.scale(twice, normal);
        const reflected = new Vector3Df(
            f(inDirection.x() - along.x()),
            f(inDirection.y() - along.y()),
            f(inDirection.z() - along.z()),
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
        inDirection: Vector3Df,
        normal: Vector3Df,
        inIndex: number,
        outIndex: number,
        totalInternalReflection: boolean[],
    ): Vector3Df {
        // [GLAS1989]: real part of the indices only
        const ratio = f(f(inIndex) / f(outIndex));
        const cosIncident = -scatteringArithmetic.dot(inDirection, normal);
        const cosTransmitted2 = f(1 + f(f(ratio * ratio) * f(f(cosIncident * cosIncident) - 1)));
        if (cosTransmitted2 < 0) {
            totalInternalReflection[0] = true;
            return ScatteringFrame.idealReflection(inDirection, normal);
        }
        totalInternalReflection[0] = false;
        const cosTransmitted = f(Math.sqrt(cosTransmitted2));
        const normalScale = f(f(ratio * cosIncident) - cosTransmitted);
        const scaled = scatteringArithmetic.scale(ratio, inDirection);
        const refracted = new Vector3Df(
            f(scaled.x() + f(normalScale * normal.x())),
            f(scaled.y() + f(normalScale * normal.y())),
            f(scaled.z() + f(normalScale * normal.z())),
        );
        return scatteringArithmetic.normalize(refracted, ScatteringFrame.NORMALIZATION_EPSILON);
    }
}
