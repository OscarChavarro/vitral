import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { QuadricSurface } from "./QuadricSurface.js";

// PovRay 1.0 expansion of a quadric along a ray, Q(o + t d) = a t^2 + b t + c,
// from products of the ray's coordinates that a renderer computes once per
// ray and reuses for every quadric it tests: the componentwise squares o*o,
// d*d and o*d and the mixed products (x y, x z, y z) of o with o, of d with d
// and the symmetric mixed product of o with d. The constant term splits into
// the part without mixed products, which a renderer may cache per viewpoint,
// and the mixed part. Roots come from QuadricSurface.solve. This is the
// evaluation order of PovRay's renderer; QuadricSurface.roots evaluates the
// same polynomial from the ray alone, in another order.
export class QuadricRayExpansion {
    // Mixed products (b.x c.y, b.x c.z, b.y c.z).
    public static mixedProducts(b: Vector3Dd, c: Vector3Dd): Vector3Dd {
        return new Vector3Dd(b.x() * c.y(), b.x() * c.z(), b.y() * c.z());
    }

    // Symmetric mixed product of origin and direction, the sum of
    // mixedProducts(direction, origin) and mixedProducts(origin, direction).
    public static symmetricMixedProducts(origin: Vector3Dd, direction: Vector3Dd): Vector3Dd {
        return QuadricRayExpansion.mixedProducts(direction, origin).add(
            QuadricRayExpansion.mixedProducts(origin, direction),
        );
    }

    // a, or zero when the quadric has no square or mixed terms.
    public static squareTerm(
        quadric: QuadricSurface,
        hasSquareTerms: boolean,
        directionSquares: Vector3Dd,
        directionMixed: Vector3Dd,
    ): number {
        if (!hasSquareTerms) return 0.0;
        let result = quadric.squareCoefficients().dotProduct(directionSquares);
        result += quadric.mixedCoefficients().dotProduct(directionMixed);
        return result;
    }

    public static linearTerm(
        quadric: QuadricSurface,
        originDirection: Vector3Dd,
        direction: Vector3Dd,
        originDirectionMixed: Vector3Dd,
    ): number {
        let result = quadric.squareCoefficients().dotProduct(originDirection);
        result *= 2.0;
        result += quadric.linearCoefficients().dotProduct(direction);
        result += quadric.mixedCoefficients().dotProduct(originDirectionMixed);
        return result;
    }

    // c without the mixed products of the origin.
    public static constantTermWithoutMixed(
        quadric: QuadricSurface,
        originSquares: Vector3Dd,
        origin: Vector3Dd,
    ): number {
        let result = quadric.squareCoefficients().dotProduct(originSquares);
        result += quadric.linearCoefficients().dotProduct(origin);
        result += quadric.constantCoefficient();
        return result;
    }

    // c from its part without mixed products.
    public static constantTerm(quadric: QuadricSurface, withoutMixed: number, originMixed: Vector3Dd): number {
        return withoutMixed + quadric.mixedCoefficients().dotProduct(originMixed);
    }
}
