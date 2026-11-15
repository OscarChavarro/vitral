import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { QuadraticSolver } from "./QuadraticSolver.js";

// Port of cpp `CubicSolver`. cpp passes `&coefficients[k]`; the port passes
// the array and the offset of its first coefficient.
export class CubicSolver {
    private static readonly TWO_PI_3 = 2.0943951023931954923084;
    private static readonly TWO_PI_43 = 4.1887902047863909846168;

    private static absoluteValue(x: number): number {
        return x < 0.0 ? 0.0 - x : x;
    }

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
        let scaleFactor: number;
        let firstNormalizedCoefficient: number;
        let secondNormalizedCoefficient: number;
        let thirdNormalizedCoefficient: number;
        const leadingCoefficient = coefficients[offset]!;
        if (leadingCoefficient === 0.0) {
            return QuadraticSolver.solve(coefficients, offset + 1, roots);
        }
        if (leadingCoefficient !== 1.0) {
            firstNormalizedCoefficient = coefficients[offset + 1]! / leadingCoefficient;
            secondNormalizedCoefficient = coefficients[offset + 2]! / leadingCoefficient;
            thirdNormalizedCoefficient = coefficients[offset + 3]! / leadingCoefficient;
        } else {
            firstNormalizedCoefficient = coefficients[offset + 1]!;
            secondNormalizedCoefficient = coefficients[offset + 2]!;
            thirdNormalizedCoefficient = coefficients[offset + 3]!;
        }
        const firstCoefficientSquared = firstNormalizedCoefficient * firstNormalizedCoefficient;
        const quadraticTerm = (firstCoefficientSquared - 3.0 * secondNormalizedCoefficient) / 9.0;
        const cubicTerm =
            (2.0 * firstCoefficientSquared * firstNormalizedCoefficient -
                9.0 * firstNormalizedCoefficient * secondNormalizedCoefficient +
                27.0 * thirdNormalizedCoefficient) /
            54.0;
        const quadraticTermCubed = quadraticTerm * quadraticTerm * quadraticTerm;
        const cubicTermSquared = cubicTerm * cubicTerm;
        let discriminant = quadraticTermCubed - cubicTermSquared;
        const offsetTerm = firstNormalizedCoefficient / 3.0;
        if (discriminant >= 0.0) {
            discriminant = cubicTerm / JavaMath.sqrt(quadraticTermCubed);
            const angle = JavaMath.acos(discriminant) / 3.0;
            scaleFactor = -2.0 * JavaMath.sqrt(quadraticTerm);
            roots[0] = scaleFactor * JavaMath.cos(angle) - offsetTerm;
            roots[1] = scaleFactor * JavaMath.cos(angle + CubicSolver.TWO_PI_3) - offsetTerm;
            roots[2] = scaleFactor * JavaMath.cos(angle + CubicSolver.TWO_PI_43) - offsetTerm;
            return 3;
        }
        scaleFactor = JavaMath.pow(
            JavaMath.sqrt(cubicTermSquared - quadraticTermCubed) + CubicSolver.absoluteValue(cubicTerm),
            1.0 / 3.0,
        );
        if (cubicTerm < 0) {
            roots[0] = scaleFactor + quadraticTerm / scaleFactor - offsetTerm;
        } else {
            roots[0] = -(scaleFactor + quadraticTerm / scaleFactor) - offsetTerm;
        }
        return 1;
    }
}
