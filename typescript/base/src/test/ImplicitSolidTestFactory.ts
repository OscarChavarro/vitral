// Port of cpp/base/src/test/ImplicitSolidTestFactory.{h,cpp}
import { PolynomialSurface } from "vsdk/toolkit/environment/geometry/volume/PolynomialSurface.js";

export class ImplicitSolidTestFactory {
    private constructor() {}

    public static createTorus(useSturmSequences: boolean): PolynomialSurface {
        const result = new PolynomialSurface(4, useSturmSequences);
        const a = result.getCoefficients();
        a[PolynomialSurface.getTermIndex(4, 4, 0, 0)] = 1;
        a[PolynomialSurface.getTermIndex(4, 0, 4, 0)] = 1;
        a[PolynomialSurface.getTermIndex(4, 0, 0, 4)] = 1;
        a[PolynomialSurface.getTermIndex(4, 2, 2, 0)] = 2;
        a[PolynomialSurface.getTermIndex(4, 2, 0, 2)] = 2;
        a[PolynomialSurface.getTermIndex(4, 0, 2, 2)] = 2;
        a[PolynomialSurface.getTermIndex(4, 2, 0, 0)] = -8.5;
        a[PolynomialSurface.getTermIndex(4, 0, 2, 0)] = -8.5;
        a[PolynomialSurface.getTermIndex(4, 0, 0, 2)] = 7.5;
        a[PolynomialSurface.getTermIndex(4, 0, 0, 0)] = 14.0625;
        return result;
    }

    public static evaluateUnivariate(coefficients: ArrayLike<number>, order: number, t: number): number {
        let result = 0;
        for (let i = 0; i <= order; ++i) result = result * t + coefficients[i]!;
        return result;
    }
}
