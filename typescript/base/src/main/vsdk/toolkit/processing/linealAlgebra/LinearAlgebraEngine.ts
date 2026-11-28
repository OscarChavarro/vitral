import type { MatrixNxM } from "../../common/linealAlgebra/MatrixNxM.js";
import { IllegalArgumentException } from "../../../../java/lang/IllegalArgumentException.js";
import type { DeterminantStrategy } from "./DeterminantStrategy.js";
import type { InverseStrategy } from "./InverseStrategy.js";
import type { LinearAlgebraBackend } from "./LinearAlgebraBackend.js";
import { ComputeStrategy, StrategySelector } from "./StrategySelector.js";

/**
Java's `strategy instanceof DeterminantStrategy`: TypeScript interfaces have
no runtime type, so the check looks for the interface's methods.
*/
const isDeterminantStrategy = (strategy: unknown): strategy is DeterminantStrategy =>
    typeof strategy === "object" &&
    strategy !== null &&
    typeof (strategy as DeterminantStrategy).id === "function" &&
    typeof (strategy as DeterminantStrategy).determinant === "function";

/** Java's `strategy instanceof InverseStrategy`, as `isDeterminantStrategy`. */
const isInverseStrategy = (strategy: unknown): strategy is InverseStrategy =>
    typeof strategy === "object" &&
    strategy !== null &&
    typeof (strategy as InverseStrategy).id === "function" &&
    typeof (strategy as InverseStrategy).inverse === "function";

/**
Java's package-private nested class `LinearAlgebraEngine.DefaultBackend`.
*/
export class DefaultBackend implements LinearAlgebraBackend {
    private readonly determinant: DeterminantStrategy;
    private readonly inverse: InverseStrategy;

    public constructor(strategy: unknown) {
        if (!isDeterminantStrategy(strategy)) {
            throw new IllegalArgumentException("strategy must implement DeterminantStrategy");
        }
        if (!isInverseStrategy(strategy)) {
            throw new IllegalArgumentException("strategy must implement InverseStrategy");
        }
        this.determinant = strategy;
        this.inverse = strategy;
    }

    public id(): string {
        return this.determinant.id();
    }

    public determinantStrategy(): DeterminantStrategy {
        return this.determinant;
    }

    public inverseStrategy(): InverseStrategy {
        return this.inverse;
    }
}

export class LinearAlgebraEngine {
    /**
    Java's `static final DEFAULT_ENGINE`. `StrategySelector` and this module
    import each other, so the instance is created on the first call instead
    of at module load; every call still returns the same instance.
    */
    private static DEFAULT_ENGINE: LinearAlgebraEngine | null = null;

    private readonly backendInstance: LinearAlgebraBackend;

    public constructor(backend: LinearAlgebraBackend | null) {
        if (backend === null || backend === undefined) {
            throw new IllegalArgumentException("backend cannot be null");
        }
        this.backendInstance = backend;
    }

    public static defaultEngine(): LinearAlgebraEngine {
        if (LinearAlgebraEngine.DEFAULT_ENGINE === null) {
            LinearAlgebraEngine.DEFAULT_ENGINE = LinearAlgebraEngine.fromStrategy(ComputeStrategy.NAIVE_COFACTOR_CPU);
        }
        return LinearAlgebraEngine.DEFAULT_ENGINE;
    }

    public static fromStrategy(strategy: ComputeStrategy | null): LinearAlgebraEngine {
        const selector: StrategySelector = new StrategySelector();
        return new LinearAlgebraEngine(selector.select(strategy));
    }

    public determinant(matrix: MatrixNxM | null): number {
        return this.backendInstance.determinantStrategy().determinant(matrix as MatrixNxM);
    }

    public inverse(matrix: MatrixNxM | null): MatrixNxM {
        return this.backendInstance.inverseStrategy().inverse(matrix as MatrixNxM);
    }

    public backend(): LinearAlgebraBackend {
        return this.backendInstance;
    }
}
