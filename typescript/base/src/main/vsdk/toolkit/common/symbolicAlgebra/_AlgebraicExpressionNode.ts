import { FundamentalEntity } from "../FundamentalEntity.js";
export abstract class _AlgebraicExpressionNode extends FundamentalEntity {
    public abstract eval(): number;
    public abstract override toString(): string;
}
