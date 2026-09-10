import type { LinearAlgebraBackend } from "./LinearAlgebraBackend.js";
import { DefaultBackend } from "./LinearAlgebraEngine.js";
import { GaussCpuStrategy } from "./GaussCpuStrategy.js";
import { LuCpuStrategy } from "./LuCpuStrategy.js";
import { NaiveCofactorCpuStrategy } from "./NaiveCofactorCpuStrategy.js";
export enum ComputeStrategy {
    NAIVE_COFACTOR_CPU,
    LU_CPU,
    GAUSS_CPU,
}
export class StrategySelector {
    public select(strategy: ComputeStrategy | null): LinearAlgebraBackend {
        switch (strategy) {
            case ComputeStrategy.LU_CPU:
                return new DefaultBackend(new LuCpuStrategy());
            case ComputeStrategy.GAUSS_CPU:
                return new DefaultBackend(new GaussCpuStrategy());
            case ComputeStrategy.NAIVE_COFACTOR_CPU:
            default:
                return new DefaultBackend(new NaiveCofactorCpuStrategy());
        }
    }
}
