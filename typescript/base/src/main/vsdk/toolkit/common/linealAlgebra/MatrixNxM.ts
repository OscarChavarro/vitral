import { FundamentalEntity } from "../FundamentalEntity.js";
import { VSDK } from "../VSDK.js";
import { MatrixDimensionMismatchException } from "./exceptions/MatrixDimensionMismatchException.js";
import { MatrixIndexOutOfBoundsException } from "./exceptions/MatrixIndexOutOfBoundsException.js";
import { Double } from "../../../../java/lang/Double.js";
import { IllegalArgumentException } from "../../../../java/lang/IllegalArgumentException.js";
import { NullPointerException } from "../../../../java/lang/NullPointerException.js";
import { LinearAlgebraEngine } from "../../processing/linealAlgebra/LinearAlgebraEngine.js";

/** Immutable general matrix whose determinant and inverse use the default linear algebra engine. */
export class MatrixNxM extends FundamentalEntity {
    private readonly rows: number;
    private readonly columns: number;
    private values: number[][];
    public constructor(rows: number, columns: number);
    public constructor(other: MatrixNxM);
    public constructor(a: number | MatrixNxM, columns?: number) {
        super();
        if (a === null) throw new NullPointerException("Matrix to copy cannot be null");
        if (a instanceof MatrixNxM) {
            this.rows = a.rows;
            this.columns = a.columns;
            this.values = MatrixNxM.deepCopy(a.values);
        } else {
            if (!Number.isInteger(a) || a <= 0 || columns === undefined || !Number.isInteger(columns) || columns <= 0)
                throw new MatrixDimensionMismatchException("Invalid matrix size: rows and columns must be > 0");
            this.rows = a;
            this.columns = columns;
            this.values = MatrixNxM.buildIdentityValues(a, columns);
        }
    }
    private static fromValues(values: number[][]): MatrixNxM {
        const r = new MatrixNxM(values.length, values[0]!.length);
        r.values = values;
        return r;
    }
    public static copyOf(other: MatrixNxM): MatrixNxM {
        return new MatrixNxM(other);
    }
    public identity(): MatrixNxM {
        return new MatrixNxM(this.rows, this.columns);
    }
    public getNumRows(): number {
        return this.rows;
    }
    public getNumColumns(): number {
        return this.columns;
    }
    public getVal(row: number, column: number): number {
        this.validatePosition(row, column);
        return this.values[row]![column]!;
    }
    public withVal(row: number, column: number, val: number): MatrixNxM {
        this.validatePosition(row, column);
        const r = MatrixNxM.deepCopy(this.values);
        r[row]![column] = val;
        return MatrixNxM.fromValues(r);
    }
    public transpose(): MatrixNxM {
        const r = Array.from({ length: this.columns }, () => Array<number>(this.rows));
        for (let i = 0; i < this.rows; i++) for (let j = 0; j < this.columns; j++) r[j]![i] = this.values[i]![j]!;
        return MatrixNxM.fromValues(r);
    }
    public multiply(value: number): MatrixNxM;
    public multiply(value: MatrixNxM): MatrixNxM;
    public multiply(value: number | MatrixNxM): MatrixNxM {
        if (typeof value === "number") return MatrixNxM.fromValues(this.values.map((row) => row.map((x) => x * value)));
        if (this.columns !== value.rows)
            throw new MatrixDimensionMismatchException(
                "When multiplying matrices, first operand number of columns must match second operand number of rows.",
            );
        const r = Array.from({ length: this.rows }, () => Array<number>(value.columns).fill(0));
        for (let i = 0; i < this.rows; i++)
            for (let j = 0; j < value.columns; j++)
                for (let k = 0; k < this.columns; k++) r[i]![j]! += this.values[i]![k]! * value.values[k]![j]!;
        return MatrixNxM.fromValues(r);
    }
    public buildMinor(row: number, column: number): MatrixNxM {
        if (this.rows <= 1 || this.columns <= 1)
            throw new MatrixDimensionMismatchException("Matrix must be at least of size 2x2 to have a minor matrix");
        this.validatePosition(row, column);
        return MatrixNxM.fromValues(
            this.values.filter((_, i) => i !== row).map((r) => r.filter((_, j) => j !== column)),
        );
    }
    public determinant(): number {
        return LinearAlgebraEngine.defaultEngine().determinant(this);
    }
    public inverse(): MatrixNxM {
        return LinearAlgebraEngine.defaultEngine().inverse(this);
    }
    public cofactors(): MatrixNxM {
        const result = new Array<number[]>(this.rows);
        for (let row = 0; row < this.rows; row++) {
            const values = new Array<number>(this.columns);
            for (let column = 0; column < this.columns; column++) {
                const minor = this.buildMinor(row, column);
                const sign = (row + column) % 2 === 0 ? 1 : -1;
                values[column] = sign * minor.determinant();
            }
            result[row] = values;
        }
        return MatrixNxM.fromValues(result);
    }
    public epsilonEquals(other: MatrixNxM | null, epsilon = VSDK.EPSILON): boolean {
        if (other === null) return false;
        if (epsilon < 0) throw new IllegalArgumentException("epsilon must be >= 0");
        if (this.rows !== other.rows || this.columns !== other.columns) return false;
        for (let i = 0; i < this.rows; i++) {
            for (let j = 0; j < this.columns; j++) {
                if (Math.abs(this.values[i]![j]! - other.values[i]![j]!) > epsilon) return false;
            }
        }
        return true;
    }
    public equals(other: unknown): boolean {
        return (
            other instanceof MatrixNxM &&
            this.rows === other.rows &&
            this.columns === other.columns &&
            this.values.every((row, i) => row.every((value, j) => Double.compare(value, other.values[i]![j]!) === 0))
        );
    }
    public hashCode(): number {
        let result = (Math.imul(31, this.rows) + this.columns) | 0;
        for (const row of this.values) {
            let rowHash = 1;
            for (const value of row) rowHash = (Math.imul(31, rowHash) + Double.hashCode(value)) | 0;
            result = (Math.imul(31, result) + rowHash) | 0;
        }
        return result;
    }
    public override toString(): string {
        return `\n------------------------------\n  - Matrix of ${this.rows} rows by ${this.columns} columns\n${this.values.map((r) => r.map((x) => VSDK.formatDouble(x)).join(" ") + " ").join("\n")}\n------------------------------\n`;
    }
    private validatePosition(row: number, column: number): void {
        if (
            !Number.isInteger(row) ||
            !Number.isInteger(column) ||
            row < 0 ||
            row >= this.rows ||
            column < 0 ||
            column >= this.columns
        )
            throw new MatrixIndexOutOfBoundsException(`Invalid matrix position [${row}][${column}]`);
    }
    private static deepCopy(values: number[][]): number[][] {
        return values.map((r) => r.slice());
    }
    private static buildIdentityValues(rows: number, columns: number): number[][] {
        return Array.from({ length: rows }, (_, i) => Array.from({ length: columns }, (_, j) => (i === j ? 1 : 0)));
    }
}
