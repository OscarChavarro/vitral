import { FundamentalEntity } from "../FundamentalEntity.js";
import { VSDK } from "../VSDK.js";
import { MatrixDimensionMismatchException } from "./exceptions/MatrixDimensionMismatchException.js";
import { MatrixIndexOutOfBoundsException } from "./exceptions/MatrixIndexOutOfBoundsException.js";
import { MatrixNotSquareException } from "./exceptions/MatrixNotSquareException.js";
import { MatrixSingularException } from "./exceptions/MatrixSingularException.js";

/** Immutable general matrix.  Elimination is local to keep this phase independent. */
export class MatrixNxM extends FundamentalEntity {
    private rows: number;
    private columns: number;
    private values: number[][];
    public constructor(rows: number, columns: number);
    public constructor(other: MatrixNxM);
    public constructor(a: number | MatrixNxM, columns?: number) {
        super();
        if (a instanceof MatrixNxM) {
            this.rows = a.rows;
            this.columns = a.columns;
            this.values = a.copyValues();
        } else {
            if (a <= 0 || columns === undefined || columns <= 0)
                throw new MatrixDimensionMismatchException("Invalid matrix size: rows and columns must be > 0");
            this.rows = a;
            this.columns = columns;
            this.values = MatrixNxM.identityValues(a, columns);
        }
    }
    private static fromValues(values: number[][]): MatrixNxM {
        const r = Object.create(MatrixNxM.prototype) as MatrixNxM;
        r.rows = values.length;
        r.columns = values[0]?.length ?? 0;
        r.values = values.map((x) => x.slice());
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
        this.position(row, column);
        return this.values[row]![column]!;
    }
    public withVal(row: number, column: number, val: number): MatrixNxM {
        this.position(row, column);
        const r = this.copyValues();
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
        this.position(row, column);
        return MatrixNxM.fromValues(
            this.values.filter((_, i) => i !== row).map((r) => r.filter((_, j) => j !== column)),
        );
    }
    public determinant(): number {
        if (this.rows !== this.columns) throw new MatrixNotSquareException("Determinant requires a square matrix");
        const a = this.copyValues();
        let sign = 1,
            det = 1;
        for (let c = 0; c < this.rows; c++) {
            let pivot = c;
            for (let r = c + 1; r < this.rows; r++) if (Math.abs(a[r]![c]!) > Math.abs(a[pivot]![c]!)) pivot = r;
            if (a[pivot]![c] === 0) return 0;
            if (pivot !== c) {
                [a[pivot], a[c]] = [a[c]!, a[pivot]!];
                sign = -sign;
            }
            const p = a[c]![c]!;
            det *= p;
            for (let r = c + 1; r < this.rows; r++) {
                const q = a[r]![c]! / p;
                for (let j = c + 1; j < this.rows; j++) a[r]![j]! -= q * a[c]![j]!;
            }
        }
        return sign * det;
    }
    public inverse(): MatrixNxM {
        if (this.rows !== this.columns) throw new MatrixNotSquareException("Inverse requires a square matrix");
        const n = this.rows,
            a = this.values.map((r, i) => [...r, ...Array.from({ length: n }, (_, j) => (i === j ? 1 : 0))]);
        for (let c = 0; c < n; c++) {
            let p = c;
            for (let r = c + 1; r < n; r++) if (Math.abs(a[r]![c]!) > Math.abs(a[p]![c]!)) p = r;
            if (Math.abs(a[p]![c]!) <= VSDK.EPSILON) throw new MatrixSingularException("Matrix is singular");
            [a[p], a[c]] = [a[c]!, a[p]!];
            const d = a[c]![c]!;
            for (let j = 0; j < 2 * n; j++) a[c]![j]! /= d;
            for (let r = 0; r < n; r++)
                if (r !== c) {
                    const q = a[r]![c]!;
                    for (let j = 0; j < 2 * n; j++) a[r]![j]! -= q * a[c]![j]!;
                }
        }
        return MatrixNxM.fromValues(a.map((r) => r.slice(n)));
    }
    public cofactors(): MatrixNxM {
        if (this.rows !== this.columns) throw new MatrixNotSquareException("Cofactors requires a square matrix");
        return MatrixNxM.fromValues(
            this.values.map((r, i) =>
                r.map((_, j) => ((i + j) % 2 === 0 ? 1 : -1) * this.buildMinor(i, j).determinant()),
            ),
        );
    }
    public epsilonEquals(other: MatrixNxM | null, epsilon = VSDK.EPSILON): boolean {
        if (other === null) return false;
        if (epsilon < 0) throw new RangeError("epsilon must be >= 0");
        if (this.rows !== other.rows || this.columns !== other.columns) return false;
        return this.values.every((r, i) => r.every((x, j) => Math.abs(x - other.values[i]![j]!) <= epsilon));
    }
    public equals(other: unknown): boolean {
        return other instanceof MatrixNxM && this.epsilonEquals(other, 0);
    }
    public override toString(): string {
        return `\n------------------------------\n  - Matrix of ${this.rows} rows by ${this.columns} columns\n${this.values.map((r) => r.map((x) => VSDK.formatDouble(x)).join(" ") + " ").join("\n")}\n------------------------------\n`;
    }
    private position(row: number, column: number): void {
        if (row < 0 || row >= this.rows || column < 0 || column >= this.columns)
            throw new MatrixIndexOutOfBoundsException(`Invalid matrix position [${row}][${column}]`);
    }
    private copyValues(): number[][] {
        return this.values.map((r) => r.slice());
    }
    private static identityValues(rows: number, columns: number): number[][] {
        return Array.from({ length: rows }, (_, i) => Array.from({ length: columns }, (_, j) => (i === j ? 1 : 0)));
    }
}
