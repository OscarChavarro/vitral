import type { MatrixNxM } from "../../common/linealAlgebra/MatrixNxM.js";
import type { DeterminantStrategy } from "./DeterminantStrategy.js";
import type { InverseStrategy } from "./InverseStrategy.js";
import type { LinearAlgebraBackend } from "./LinearAlgebraBackend.js";
import { ComputeStrategy, StrategySelector } from "./StrategySelector.js";

type CompleteStrategy = DeterminantStrategy & InverseStrategy;
export class DefaultBackend implements LinearAlgebraBackend {
    public constructor(private readonly strategy: CompleteStrategy) {
        if (strategy === null) throw new RangeError("strategy must implement DeterminantStrategy and InverseStrategy");
    }
    public id(): string {
        return this.strategy.id();
    }
    public determinantStrategy(): DeterminantStrategy {
        return this.strategy;
    }
    public inverseStrategy(): InverseStrategy {
        return this.strategy;
    }
}
export class LinearAlgebraEngine {
    private static defaultEngineInstance: LinearAlgebraEngine | null = null;
    public constructor(private readonly selectedBackend: LinearAlgebraBackend) {
        if (selectedBackend === null) throw new RangeError("backend cannot be null");
    }
    public static defaultEngine(): LinearAlgebraEngine {
        this.defaultEngineInstance ??= this.fromStrategy(ComputeStrategy.NAIVE_COFACTOR_CPU);
        return this.defaultEngineInstance;
    }
    public static fromStrategy(strategy: ComputeStrategy): LinearAlgebraEngine {
        return new LinearAlgebraEngine(new StrategySelector().select(strategy));
    }
    public determinant(matrix: MatrixNxM): number {
        return this.selectedBackend.determinantStrategy().determinant(matrix);
    }
    public inverse(matrix: MatrixNxM): MatrixNxM {
        return this.selectedBackend.inverseStrategy().inverse(matrix);
    }
    public backend(): LinearAlgebraBackend {
        return this.selectedBackend;
    }
}
