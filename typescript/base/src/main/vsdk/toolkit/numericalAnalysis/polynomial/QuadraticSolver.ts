import { Math as JavaMath } from "../../../../java/lang/Math.js";

// Port of cpp `QuadraticSolver`. cpp passes `&coefficients[k]`; the port
// passes the array and the offset of its first coefficient.
export class QuadraticSolver {
    private static readonly COEFFICIENT_LIMIT = 1.0e-20;

    public static solve(coefficients: ArrayLike<number>, roots: number[]): number;
    public static solve(coefficients: ArrayLike<number>, offset: number, roots: number[]): number;
    public static solve(
        coefficients: ArrayLike<number>,
        offsetOrRoots: number | number[],
        maybeRoots?: number[],
    ): number {
        let offset: number;
        let roots: number[];
        if (typeof offsetOrRoots === "number") {
            offset = offsetOrRoots;
            roots = maybeRoots as number[];
        } else {
            offset = 0;
            roots = offsetOrRoots;
        }
        const quadraticCoefficient = coefficients[offset]!;
        const linearCoefficient = -coefficients[offset + 1]!;
        const constantCoefficient = coefficients[offset + 2]!;
        if (quadraticCoefficient === 0.0) {
            if (linearCoefficient === 0.0) {
                return 0;
            }
            roots[0] = constantCoefficient / linearCoefficient;
            return 1;
        }
        let discriminant = linearCoefficient * linearCoefficient - 4.0 * quadraticCoefficient * constantCoefficient;
        if (discriminant < 0.0) {
            return 0;
        }
        if (JavaMath.abs(discriminant) < QuadraticSolver.COEFFICIENT_LIMIT) {
            roots[0] = (0.5 * linearCoefficient) / quadraticCoefficient;
            return 1;
        }
        discriminant = JavaMath.sqrt(discriminant);
        const denominator = 2.0 * quadraticCoefficient;
        roots[0] = (linearCoefficient + discriminant) / denominator;
        roots[1] = (linearCoefficient - discriminant) / denominator;
        return 2;
    }
}
