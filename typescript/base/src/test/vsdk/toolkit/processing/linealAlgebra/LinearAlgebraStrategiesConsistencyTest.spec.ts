import { describe, expect, it } from "vitest";
import { MatrixNxM } from "vsdk/toolkit/common/linealAlgebra/MatrixNxM.js";
import { LinearAlgebraEngine } from "vsdk/toolkit/processing/linealAlgebra/LinearAlgebraEngine.js";
import { ComputeStrategy } from "vsdk/toolkit/processing/linealAlgebra/StrategySelector.js";

const strategies = [ComputeStrategy.NAIVE_COFACTOR_CPU, ComputeStrategy.LU_CPU, ComputeStrategy.GAUSS_CPU];

describe("LinearAlgebraStrategiesConsistencyTest", () => {
    it.each(strategies)("given same matrix, all determinant strategies agree (%s)", (strategy) => {
        const matrix = new MatrixNxM(3, 3)
            .withVal(0, 0, 3)
            .withVal(0, 1, 2)
            .withVal(0, 2, -1)
            .withVal(1, 0, 2)
            .withVal(1, 1, -2)
            .withVal(1, 2, 4)
            .withVal(2, 0, -1)
            .withVal(2, 1, 0.5)
            .withVal(2, 2, -1);
        const expected = LinearAlgebraEngine.fromStrategy(ComputeStrategy.NAIVE_COFACTOR_CPU).determinant(matrix);
        expect(LinearAlgebraEngine.fromStrategy(strategy).determinant(matrix)).toBeCloseTo(expected, 9);
    });

    it.each(strategies)("given invertible matrix, all inverse strategies agree (%s)", (strategy) => {
        const matrix = new MatrixNxM(3, 3)
            .withVal(0, 0, 4)
            .withVal(0, 1, 7)
            .withVal(0, 2, 2)
            .withVal(1, 0, 3)
            .withVal(1, 1, 6)
            .withVal(1, 2, 1)
            .withVal(2, 0, 2)
            .withVal(2, 1, 5)
            .withVal(2, 2, 3);
        const expected = LinearAlgebraEngine.fromStrategy(ComputeStrategy.NAIVE_COFACTOR_CPU).inverse(matrix);
        const actual = LinearAlgebraEngine.fromStrategy(strategy).inverse(matrix);
        expect(actual.epsilonEquals(expected, 1e-7)).toBe(true);
        expect(matrix.multiply(actual).epsilonEquals(new MatrixNxM(3, 3), 1e-6)).toBe(true);
    });
});
