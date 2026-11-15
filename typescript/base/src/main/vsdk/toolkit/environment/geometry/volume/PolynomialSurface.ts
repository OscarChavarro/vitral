import { Double } from "../../../../../java/lang/Double.js";
import { IllegalArgumentException } from "../../../../../java/lang/IllegalArgumentException.js";
import type { ArrayList } from "../../../../../java/util/ArrayList.js";
import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { FloatingInterval } from "../../../common/numerics/FloatingInterval.js";
import { PolynomialSolver } from "../../../numericalAnalysis/polynomial/PolynomialSolver.js";
import { QuadraticSolver } from "../../../numericalAnalysis/polynomial/QuadraticSolver.js";
import { QuarticSolver } from "../../../numericalAnalysis/polynomial/QuarticSolver.js";
import type { GeometryBounds } from "../element/GeometryBounds.js";
import type { Geometry } from "../Geometry.js";
import { ImplicitSolid } from "./ImplicitSolid.js";

// Implicit solid F(x,y,z) < 0 of a polynomial of order 2 to 7 in x, y and z.
// Coefficients are stored in the PovRay term order: decreasing powers of x,
// then of y, then of z, so a quartic's 35 terms run from x^4 (index 0) down to
// the constant (index 34). getTermIndex and getTermPowers convert between term
// indices and powers. Ray roots come from the Sturm-sequence solver, or for
// quartics from the closed-form solver unless Sturm sequences are requested.
// The surface is generally unbounded; bound it by composition when needed.
export class PolynomialSurface extends ImplicitSolid {
    public static readonly MINIMUM_ORDER = 2;
    public static readonly MAXIMUM_ORDER = 7;
    public static readonly MAXIMUM_TERMS = 120;
    // Root solver tolerance of PovRay polynomial shapes.
    public static readonly SOLVER_EPSILON = 1.0e-10;
    // PovRay drops expanded ray coefficients of magnitude below this value.
    public static readonly POVRAY_EXPANSION_CUTOFF = 1.0e-4;

    private static readonly COEFFICIENT_LIMIT = 1.0e-20;
    private static readonly TERM_COUNTS: readonly number[] = [1, 4, 10, 20, 35, 56, 84, 120];
    private static readonly BINOMIAL: readonly (readonly number[])[] = [
        [0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
        [0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0],
        [0, 1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0],
        [0, 1, 3, 3, 1, 0, 0, 0, 0, 0, 0, 0],
        [0, 1, 4, 6, 4, 1, 0, 0, 0, 0, 0, 0],
        [0, 1, 5, 10, 10, 5, 1, 0, 0, 0, 0, 0],
        [0, 1, 6, 15, 20, 15, 6, 1, 0, 0, 0, 0],
        [0, 1, 7, 21, 35, 35, 21, 7, 1, 0, 0, 0],
        [0, 1, 8, 28, 56, 70, 56, 28, 8, 1, 0, 0],
        [0, 1, 9, 36, 84, 126, 126, 84, 36, 9, 1, 0],
        [0, 1, 10, 45, 120, 210, 252, 210, 120, 45, 10, 1],
    ];
    private static readonly FACTORIALS: readonly number[] = [1, 1, 2, 6, 24, 120, 720, 5040];

    private order: number;
    private sturm: boolean;
    private coefficients: number[];

    private static rollTerm(order: number, x: number, y: number, z: number): number {
        const B = PolynomialSurface.BINOMIAL;
        const xstart = B[order - x + 2]![order - x]!;
        order = order - x;
        const ystart = B[order - y + 1]![order - y]!;
        order = order - y;
        const zstart = B[order - z]![order - z]!;
        return xstart + ystart + zstart;
    }

    // Substitutes the linear map q into the polynomial coeffs of the given order.
    // q is a one-element array (cpp Matrix4x4d*).
    private static transform(order: number, coeffs: number[], q: Matrix4x4d[], cutoff: number): void {
        const TERM_COUNTS = PolynomialSurface.TERM_COUNTS;
        const LIMIT = PolynomialSurface.COEFFICIENT_LIMIT;
        let termIndex: number;
        let partialIndex: number;
        const ip: number[] = [0];
        let i: number;
        const i0: number[] = [0];
        const i1: number[] = [0];
        const i2: number[] = [0];
        const i3: number[] = [0];
        const jp: number[] = [0];
        let j: number;
        const j0: number[] = [0];
        const j1: number[] = [0];
        const j2: number[] = [0];
        const j3: number[] = [0];
        const kp: number[] = [0];
        let k: number;
        const k0: number[] = [0];
        const k1: number[] = [0];
        const k2: number[] = [0];
        const k3: number[] = [0];
        const wp: number[] = [0];
        const b: number[] = new Array<number>(PolynomialSurface.MAXIMUM_TERMS).fill(0);
        let term: number;
        let tempx: number;
        let tempy: number;
        let tempz: number;

        for (i = 0; i < 4; i++) {
            for (j = 0; j < 4; j++) {
                if (q[0]!.get(i, j) > -LIMIT && q[0]!.get(i, j) < LIMIT) {
                    q[0] = q[0]!.withVal(i, j, 0.0);
                }
            }
        }

        for (i = 0; i < TERM_COUNTS[order]!; i++) {
            b[i] = 0.0;
        }
        for (termIndex = 0; termIndex < TERM_COUNTS[order]!; termIndex++) {
            if (coeffs[termIndex] !== 0.0) {
                PolynomialSurface.getTermPowers(order, termIndex, ip, jp, kp, wp);
                // Step through terms in: (q[0][0]*x+q[0][1]*y+q[0][2]*z+q[0][3])^i
                for (i = 0; i < TERM_COUNTS[ip[0]!]!; i++) {
                    PolynomialSurface.getTermPowers(ip[0]!, i, i0, i1, i2, i3);
                    tempx = PolynomialSurface.partialTerm(q[0]!, 0, ip[0]!, i0[0]!, i1[0]!, i2[0]!, i3[0]!);
                    if (tempx !== 0.0) {
                        // Step through terms in:
                        // (q[1][0]*x+q[1][1]*y+q[1][2]*z+q[1][3])^j
                        for (j = 0; j < TERM_COUNTS[jp[0]!]!; j++) {
                            PolynomialSurface.getTermPowers(jp[0]!, j, j0, j1, j2, j3);
                            tempy = PolynomialSurface.partialTerm(q[0]!, 1, jp[0]!, j0[0]!, j1[0]!, j2[0]!, j3[0]!);
                            if (tempy !== 0.0) {
                                // Step through terms in:
                                // (q[2][0]*x+q[2][1]*y+q[2][2]*z+q[2][3])^k
                                for (k = 0; k < TERM_COUNTS[kp[0]!]!; k++) {
                                    PolynomialSurface.getTermPowers(kp[0]!, k, k0, k1, k2, k3);
                                    tempz = PolynomialSurface.partialTerm(
                                        q[0]!,
                                        2,
                                        kp[0]!,
                                        k0[0]!,
                                        k1[0]!,
                                        k2[0]!,
                                        k3[0]!,
                                    );
                                    if (tempz !== 0.0) {
                                        // Figure out it's index, and add into
                                        // result
                                        partialIndex = PolynomialSurface.rollTerm(
                                            order,
                                            i0[0]! + j0[0]! + k0[0]!,
                                            i1[0]! + j1[0]! + k1[0]!,
                                            i2[0]! + j2[0]! + k2[0]!,
                                        );
                                        term = coeffs[termIndex]! * tempx * tempy * tempz;
                                        b[partialIndex]! += term;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        for (i = 0; i < TERM_COUNTS[order]!; i++) {
            if (b[i]! > -cutoff && b[i]! < cutoff) {
                coeffs[i] = 0.0;
            } else {
                coeffs[i] = b[i]!;
            }
        }
    }

    private static partialTerm(
        q: Matrix4x4d,
        row: number,
        pwr: number,
        i: number,
        j: number,
        k: number,
        l: number,
    ): number {
        const F = PolynomialSurface.FACTORIALS;
        // cpp integer division
        let result = Math.trunc(F[pwr]! / (F[i]! * F[j]! * F[k]! * F[l]!));
        for (let n = 0; n < i; n++) result *= q.get(0, row);
        for (let n = 0; n < j; n++) result *= q.get(1, row);
        for (let n = 0; n < k; n++) result *= q.get(2, row);
        for (let n = 0; n < l; n++) result *= q.get(3, row);
        return result;
    }

    private intersectGeneral(
        origin: Vector3Dd,
        direction: Vector3Dd,
        minimumDistance: number,
        depths: number[],
        cutoff: number,
    ): number {
        const t: number[] = new Array<number>(PolynomialSurface.MAXIMUM_ORDER + 1).fill(0);
        let i: number;
        let j: number;
        this.getRayPolynomial(origin, direction, t, cutoff);
        for (i = 0; i <= this.order; i++) {
            if (t[i]! > -PolynomialSurface.COEFFICIENT_LIMIT && t[i]! < PolynomialSurface.COEFFICIENT_LIMIT) {
                t[i] = 0.0;
            }
        }
        for (i = 0, j = this.order; i <= this.order; i++) {
            if (t[i] !== 0.0) {
                break;
            }
            j -= 1;
        }
        if (j > 2) {
            const polynomialSolver = new PolynomialSolver(j, t, i);
            return polynomialSolver.solve(depths, minimumDistance, PolynomialSurface.SOLVER_EPSILON);
        }
        // The three lowest coefficients; leading entries are zero when j < 2.
        if (j > 0) {
            return QuadraticSolver.solve(t, this.order - 2, depths);
        }
        return 0;
    }

    private intersectQuartic(
        origin: Vector3Dd,
        direction: Vector3Dd,
        minimumDistance: number,
        depths: number[],
    ): number {
        const t: number[] = [0, 0, 0, 0, 0];
        this.getQuarticRayPolynomial(origin, direction, t);
        if (this.sturm) {
            if (t[0]! === 0.0) {
                if (t[1]! === 0.0) {
                    return QuadraticSolver.solve(t, 2, depths);
                }
                const cubicSolver = new PolynomialSolver(3, t, 1);
                return cubicSolver.solve(depths, minimumDistance, PolynomialSurface.SOLVER_EPSILON);
            }
            const quarticSolver = new PolynomialSolver(4, t, 0);
            return quarticSolver.solve(depths, minimumDistance, PolynomialSurface.SOLVER_EPSILON);
        }
        return QuarticSolver.solve(t, depths, minimumDistance, PolynomialSurface.SOLVER_EPSILON);
    }

    private quarticGradient(point: Vector3Dd): Vector3Dd {
        const a = this.coefficients;
        const x = point.x();
        const y = point.y();
        const z = point.z();
        const x2 = x * x;
        const y2 = y * y;
        const z2 = z * z;
        const x3 = x * x2;
        const y3 = y * y2;
        const z3 = z * z2;
        const nx =
            4 * a[0]! * x3 +
            3 * x2 * (a[1]! * y + a[2]! * z + a[3]!) +
            2 * x * (a[4]! * y2 + y * (a[5]! * z + a[6]!) + a[7]! * z2 + a[8]! * z + a[9]!) +
            a[10]! * y3 +
            y2 * (a[11]! * z + a[12]!) +
            y * (a[13]! * z2 + a[14]! * z + a[15]!) +
            a[16]! * z3 +
            a[17]! * z2 +
            a[18]! * z +
            a[19]!;

        const ny =
            a[1]! * x3 +
            x2 * (2 * a[4]! * y + a[5]! * z + a[6]!) +
            x * (3 * a[10]! * y2 + 2 * y * (a[11]! * z + a[12]!) + a[13]! * z2 + a[14]! * z + a[15]!) +
            4 * a[20]! * y3 +
            3 * y2 * (a[21]! * z + a[22]!) +
            2 * y * (a[23]! * z2 + a[24]! * z + a[25]!) +
            a[26]! * z3 +
            a[27]! * z2 +
            a[28]! * z +
            a[29]!;

        const nz =
            a[2]! * x3 +
            x2 * (a[5]! * y + 2 * a[7]! * z + a[8]!) +
            x * (a[11]! * y2 + y * (2 * a[13]! * z + a[14]!) + 3 * a[16]! * z2 + 2 * a[17]! * z + a[18]!) +
            a[21]! * y3 +
            y2 * (2 * a[23]! * z + a[24]!) +
            y * (3 * a[26]! * z2 + 2 * a[27]! * z + a[28]!) +
            4 * a[30]! * z3 +
            3 * a[31]! * z2 +
            2 * a[32]! * z +
            a[33]!;
        return new Vector3Dd(nx, ny, nz);
    }

    private static power(value: FloatingInterval, exponent: number): FloatingInterval {
        if (exponent === 0) return FloatingInterval.exact(1.0);
        if (exponent === 1) return value;
        if (exponent % 2 === 0) return PolynomialSurface.power(value.square(), exponent / 2);
        return value.multiply(PolynomialSurface.power(value, exponent - 1));
    }

    // All coefficients start at zero. Throws java::IllegalArgumentException
    // for an order outside [MINIMUM_ORDER, MAXIMUM_ORDER].
    public constructor(initialOrder: number, useSturmSequences: boolean);
    /** cpp implicit copy constructor. */
    public constructor(other: PolynomialSurface);
    public constructor(initialOrderOrOther: number | PolynomialSurface, useSturmSequences?: boolean) {
        super();
        if (initialOrderOrOther instanceof PolynomialSurface) {
            const other = initialOrderOrOther;
            this.order = other.order;
            this.sturm = other.sturm;
            this.coefficients = other.coefficients.slice();
            return;
        }
        const initialOrder = initialOrderOrOther;
        this.order = initialOrder;
        this.sturm = useSturmSequences as boolean;
        this.coefficients = new Array<number>(PolynomialSurface.MAXIMUM_TERMS).fill(0.0);
        if (!PolynomialSurface.isValidOrder(initialOrder))
            throw new IllegalArgumentException("PolynomialSurface order must be in [2, 7]");
    }

    public override copyGeometry(): Geometry {
        return new PolynomialSurface(this);
    }

    public static isValidOrder(value: number): boolean {
        return value >= PolynomialSurface.MINIMUM_ORDER && value <= PolynomialSurface.MAXIMUM_ORDER;
    }

    public static getTermCount(value: number): number {
        return value >= 0 && value <= PolynomialSurface.MAXIMUM_ORDER ? PolynomialSurface.TERM_COUNTS[value]! : 0;
    }

    // Term index of x^xPower y^yPower z^zPower, or -1 when the powers are
    // negative or add up to more than order.
    public static getTermIndex(value: number, x: number, y: number, z: number): number {
        if (value < 0 || value > PolynomialSurface.MAXIMUM_ORDER || x < 0 || y < 0 || z < 0 || x + y + z > value)
            return -1;
        return PolynomialSurface.rollTerm(value, x, y, z);
    }

    // Powers of term index; constantPower is order minus the other three.
    // Outputs are one-element arrays.
    public static getTermPowers(
        order: number,
        index: number,
        x: number[],
        y: number[],
        z: number[],
        w: number[],
    ): void {
        const B = PolynomialSurface.BINOMIAL;
        let i: number;
        let torder: number;
        if (index === 0) {
            x[0] = order;
            y[0] = 0;
            z[0] = 0;
            w[0] = 0;
            return;
        }
        if (order === 1) {
            if (index === 1) {
                x[0] = 0;
                y[0] = 1;
                z[0] = 0;
                w[0] = 0;
                return;
            }
            x[0] = 0;
            y[0] = 0;
            z[0] = 3 - index;
            w[0] = order - (x[0]! + y[0]! + z[0]!);
            return;
        }
        for (i = 0; B[3 + i]![i + 1]! <= index; i++) {
            // empty
        }
        torder = order;
        x[0] = torder - i;
        index -= B[2 + i]![i]!;
        torder = i;
        if (index === 0) {
            y[0] = torder;
            z[0] = 0;
            w[0] = 0;
            return;
        }
        if (torder === 1) {
            y[0] = 0;
            z[0] = 2 - index;
            w[0] = order - (x[0]! + y[0]! + z[0]!);
            return;
        }
        for (i = 0; B[2 + i]![i + 1]! <= index; i++) {
            // empty
        }
        y[0] = torder - i;
        index -= B[1 + i]![i]!;
        torder = i;
        z[0] = torder - index;
        w[0] = order - (x[0]! + y[0]! + z[0]!);
    }

    public getOrder(): number {
        return this.order;
    }

    public usesSturmSequences(): boolean {
        return this.sturm;
    }

    public getCoefficients(): number[] {
        return this.coefficients;
    }

    // PovRay ray roots: distances d >= minimumDistance along the unit direction
    // where the surface is crossed, in solver order; returns their count (at
    // most order). Orders other than 4 drop expanded coefficients below
    // POVRAY_EXPANSION_CUTOFF; the typed queries keep them.
    public intersect(origin: Vector3Dd, unitDirection: Vector3Dd, minimumDistance: number, depths: number[]): number {
        if (this.order === 4) return this.intersectQuartic(origin, unitDirection, minimumDistance, depths);
        return this.intersectGeneral(
            origin,
            unitDirection,
            minimumDistance,
            depths,
            PolynomialSurface.POVRAY_EXPANSION_CUTOFF,
        );
    }

    // Coefficients of t^order down to t^0 of F(origin + t*direction), by
    // general term expansion, zeroing those below cutoff in magnitude; the
    // quartic solver path uses its own expansion.
    public getRayPolynomial(origin: Vector3Dd, direction: Vector3Dd, result: number[], cutoff: number): void {
        const a: number[] = new Array<number>(PolynomialSurface.MAXIMUM_TERMS).fill(0);
        for (let i = 0; i < PolynomialSurface.TERM_COUNTS[this.order]!; i++) a[i] = this.coefficients[i]!;
        let q = Matrix4x4d.identityMatrix().multiply(0.0);
        q = q.withVal(0, 0, direction.x());
        q = q.withVal(3, 0, origin.x());
        q = q.withVal(0, 1, direction.y());
        q = q.withVal(3, 1, origin.y());
        q = q.withVal(0, 2, direction.z());
        q = q.withVal(3, 2, origin.z());
        const qHolder: Matrix4x4d[] = [q];
        PolynomialSurface.transform(this.order, a, qHolder, cutoff);
        // The polynomial is now in x alone: x^(order-i) holds the t^(order-i) term.
        for (let i = 0; i <= this.order; i++) result[i] = a[PolynomialSurface.BINOMIAL[3 + i]![4]! - 1]!;
    }

    public getQuarticRayPolynomial(origin: Vector3Dd, direction: Vector3Dd, result: number[]): void {
        const a = this.coefficients;
        const x = origin.x();
        const y = origin.y();
        const z = origin.z();
        const xx = direction.x();
        const yy = direction.y();
        const zz = direction.z();
        const x2 = x * x;
        const y2 = y * y;
        const z2 = z * z;
        const x3 = x * x2;
        const y3 = y * y2;
        const z3 = z * z2;
        const x4 = x * x3;
        const y4 = y * y3;
        const z4 = z * z3;
        const xx2 = xx * xx;
        const yy2 = yy * yy;
        const zz2 = zz * zz;
        const xx3 = xx * xx2;
        const yy3 = yy * yy2;
        const zz3 = zz * zz2;
        const xx4 = xx * xx3;
        const yy4 = yy * yy3;
        const zz4 = zz * zz3;
        const xZ = x * z;
        const xZz = x * zz;
        const xxZ = xx * z;
        const xxZz = xx * zz;
        const xY = x * y;
        const xYy = x * yy;
        const xxY = xx * y;
        const xxYy = xx * yy;
        const yZ = y * z;
        const yZz = y * zz;
        const yyZ = yy * z;
        const yyZz = yy * zz;

        /**
    Determine the coefficients of t^n, where the line is represented
    as (x,y,z) + (xx,yy,zz)*t.
    */
        let temp = a[0]! * xx4;
        temp += a[1]! * xx3 * yy;
        temp += a[2]! * xx3 * zz;
        temp += a[4]! * xx2 * yy2;
        temp += a[5]! * xx2 * yyZz;
        temp += a[7]! * xx2 * zz2;
        temp += a[10]! * xx * yy3;
        temp += a[11]! * xxZz * yy2;
        temp += a[13]! * xxYy * zz2;
        temp += a[16]! * xx * zz3;
        temp += a[20]! * yy4;
        temp += a[21]! * yy3 * zz;
        temp += a[23]! * yy2 * zz2;
        temp += a[26]! * yy * zz3;
        temp += a[30]! * zz4;

        result[0] = temp;

        temp = 4 * a[0]! * x * xx3;
        temp += a[1]! * (3 * xx2 * xYy + xx3 * y);
        temp += a[2]! * (3 * xx2 * xZz + xx3 * z);
        temp += a[3]! * xx3;
        temp += a[4]! * (2 * x * xx * yy2 + 2 * xx2 * y * yy);
        temp += a[5]! * (xx2 * (yZz + yyZ) + 2 * xYy * xxZz);
        temp += a[6]! * xx2 * yy;
        temp += a[7]! * (2 * x * xx * zz2 + 2 * xx2 * z * zz);
        temp += a[8]! * xx2 * zz;
        temp += a[10]! * (x * yy3 + 3 * xxY * yy2);
        temp += a[11]! * (xx * (2 * y * yyZz + yy2 * z) + xZz * yy2);
        temp += a[12]! * xx * yy2;
        temp += a[13]! * (xx * (y * zz2 + 2 * yyZ * zz) + xYy * zz2);
        temp += a[14]! * xxYy * zz;
        temp += a[16]! * (x * zz3 + 3 * xxZ * zz2);
        temp += a[17]! * xx * zz2;
        temp += 4 * a[20]! * y * yy3;
        temp += a[21]! * (3 * yy2 * yZz + yy3 * z);
        temp += a[22]! * yy3;
        temp +=
            zz *
            (2 * a[23]! * yy * (yZz + yyZ) +
                a[24]! * yy2 +
                zz * (a[26]! * (yZz + 3 * yyZ) + a[27]! * yy + zz * (4 * a[30]! * z + a[31]!)));
        result[1] = temp;

        temp = 6 * a[0]! * x2 * xx2;
        temp += 3 * a[1]! * x * xx * (xYy + xxY);
        temp += 3 * a[2]! * x * xx * (xZz + xxZ);
        temp += 3 * a[3]! * x * xx2;
        temp += a[4]! * (x2 * yy2 + 4 * xYy * xxY + xx2 * y2);
        temp += a[5]! * (x2 * yyZz + 2 * x * xx * (yZz + yyZ) + xx2 * yZ);
        temp += a[6]! * (2 * x * xxYy + xx2 * y);
        temp += a[7]! * (x2 * zz2 + 4 * xZz * xxZ + xx2 * z2);
        temp += a[8]! * (2 * x * xxZz + xx2 * z);
        temp += a[9]! * xx2;
        temp += a[10]! * (3 * xY * yy2 + 3 * xxYy * y2);
        temp += a[11]! * (xYy * (2 * yZz + yyZ) + xx * (y2 * zz + 2 * y * yyZ));
        temp += a[12]! * (x * yy2 + 2 * xxY * yy);
        temp += a[13]! * (xZz * (yZz + 2 * yyZ) + xx * (2 * yZ * zz + yy * z2));
        temp += a[14]! * (xYy * zz + xx * (yZz + yyZ));
        temp += a[15]! * xxYy;
        temp += a[16]! * (3 * xZ * zz2 + 3 * xxZz * z2);
        temp += a[17]! * (x * zz2 + 2 * xxZ * zz);
        temp += a[18]! * xxZz;
        temp += 6 * a[20]! * y2 * yy2;
        temp += 3 * a[21]! * y * yy * (yZz + yyZ);
        temp += 3 * a[22]! * y * yy2;
        temp += a[23]! * (y2 * zz2 + 4 * yZz * yyZ + yy2 * z2);
        temp += a[24]! * (2 * y * yyZz + yy2 * z);
        temp += a[25]! * yy2;
        temp +=
            zz *
            (3 * a[26]! * z * (yZz + yyZ) +
                a[27]! * (yZz + 2 * yyZ) +
                a[28]! * yy +
                6 * a[30]! * z2 * zz +
                3 * a[31]! * z * zz +
                a[32]! * zz);
        result[2] = temp;

        temp = 4 * a[0]! * x3 * xx;
        temp += a[1]! * x2 * (xYy + 3 * xxY);
        temp += a[2]! * x2 * (xZz + 3 * xxZ);
        temp += 3 * a[3]! * x2 * xx;
        temp += 2 * a[4]! * xY * (xYy + xxY);
        temp += a[5]! * x * (x * (yZz + yyZ) + 2 * xxY * z);
        temp += a[6]! * x * (xYy + 2 * xxY);
        temp += 2 * a[7]! * xZ * (xZz + xxZ);
        temp += a[8]! * x * (xZz + 2 * xxZ);
        temp += 2 * a[9]! * x * xx;
        temp += a[10]! * (3 * xYy * y2 + xx * y3);
        temp += a[11]! * (xY * (yZz + 2 * yyZ) + xxZ * y2);
        temp += a[12]! * (2 * xY * yy + xx * y2);
        temp += a[13]! * (xZ * (2 * yZz + yyZ) + xxY * z2);
        temp += a[14]! * (x * (yZz + yyZ) + xxY * z);
        temp += a[15]! * (xYy + xxY);
        temp += a[16]! * (3 * xZz * z2 + xx * z3);
        temp += a[17]! * (2 * xZ * zz + xx * z2);
        temp += a[18]! * (xZz + xxZ);
        temp += a[19]! * xx;
        temp += 4 * a[20]! * y3 * yy;
        temp += a[21]! * y2 * (yZz + 3 * yyZ);
        temp += 3 * a[22]! * y2 * yy;
        temp += 2 * a[23]! * yZ * (yZz + yyZ);
        temp += a[24]! * y * (yZz + 2 * yyZ);
        temp += 2 * a[25]! * y * yy;
        temp += a[26]! * (3 * yZz * z2 + yy * z3);
        temp += a[27]! * (2 * yZ * zz + yy * z2);
        temp += a[28]! * (yZz + yyZ);
        temp += a[29]! * yy;
        temp += zz * (4 * a[30]! * z3 + 3 * a[31]! * z2 + 2 * a[32]! * z + a[33]!);
        result[3] = temp;

        temp = a[0]! * x4;
        temp += a[1]! * x3 * y;
        temp += a[2]! * x3 * z;
        temp += a[3]! * x3;
        temp += a[4]! * x2 * y2;
        temp += a[5]! * x2 * yZ;
        temp += a[6]! * x2 * y;
        temp += a[7]! * x2 * z2;
        temp += a[8]! * x2 * z;
        temp += a[9]! * x2;
        temp += a[10]! * x * y3;
        temp += a[11]! * x * y2 * z;
        temp += a[12]! * x * y2;
        temp += a[13]! * xY * z2;
        temp += a[14]! * xY * z;
        temp += a[15]! * xY;
        temp += a[16]! * x * z3;
        temp += a[17]! * x * z2;
        temp += a[18]! * xZ;
        temp += a[19]! * x;
        temp += a[20]! * y4;
        temp += a[21]! * y3 * z;
        temp += a[22]! * y3;
        temp += a[23]! * y2 * z2;
        temp += a[24]! * y2 * z;
        temp += a[25]! * y2;
        temp += a[26]! * y * z3;
        temp += a[27]! * y * z2;
        temp += a[28]! * yZ;
        temp += a[29]! * y;
        temp += a[30]! * z4;
        temp += a[31]! * z3;
        temp += a[32]! * z2;
        temp += a[33]! * z;
        temp += a[34]!;
        result[4] = temp;
    }

    public evaluate(point: Vector3Dd): number {
        const MAX = PolynomialSurface.MAXIMUM_ORDER;
        const x: number[] = new Array<number>(MAX + 1).fill(0);
        const y: number[] = new Array<number>(MAX + 1).fill(0);
        const z: number[] = new Array<number>(MAX + 1).fill(0);
        const k0: number[] = [0];
        const k1: number[] = [0];
        const k2: number[] = [0];
        const k3: number[] = [0];
        x[0] = 1.0;
        y[0] = 1.0;
        z[0] = 1.0;
        x[1] = point.x();
        y[1] = point.y();
        z[1] = point.z();
        for (let i = 2; i <= MAX; i++) {
            x[i] = x[1]! * x[i - 1]!;
            y[i] = y[1]! * y[i - 1]!;
            z[i] = z[1]! * z[i - 1]!;
        }
        let result = 0.0;
        for (let i = 0; i < PolynomialSurface.TERM_COUNTS[this.order]!; i++) {
            PolynomialSurface.getTermPowers(this.order, i, k0, k1, k2, k3);
            result += this.coefficients[i]! * x[k0[0]!]! * y[k1[0]!]! * z[k2[0]!]!;
        }
        return result;
    }

    public gradient(point: Vector3Dd): Vector3Dd {
        if (this.order === 4) return this.quarticGradient(point);
        const xp: number[] = [0];
        const yp: number[] = [0];
        const zp: number[] = [0];
        const wp: number[] = [0];
        const MAX = PolynomialSurface.MAXIMUM_ORDER;
        const x: number[] = new Array<number>(MAX + 1).fill(0);
        const y: number[] = new Array<number>(MAX + 1).fill(0);
        const z: number[] = new Array<number>(MAX + 1).fill(0);
        x[0] = 1.0;
        y[0] = 1.0;
        z[0] = 1.0;
        x[1] = point.x();
        y[1] = point.y();
        z[1] = point.z();
        for (let i = 2; i <= this.order; i++) {
            x[i] = point.x() * x[i - 1]!;
            y[i] = point.y() * y[i - 1]!;
            z[i] = point.z() * z[i - 1]!;
        }
        const a = this.coefficients;
        let rx = 0.0;
        let ry = 0.0;
        let rz = 0.0;
        for (let i = 0; i < PolynomialSurface.TERM_COUNTS[this.order]!; i++) {
            PolynomialSurface.getTermPowers(this.order, i, xp, yp, zp, wp);
            if (xp[0]! >= 1) {
                rx += xp[0]! * a[i]! * x[xp[0]! - 1]! * y[yp[0]!]! * z[zp[0]!]!;
            }
            if (yp[0]! >= 1) {
                ry += yp[0]! * a[i]! * x[xp[0]!]! * y[yp[0]! - 1]! * z[zp[0]!]!;
            }
            if (zp[0]! >= 1) {
                rz += zp[0]! * a[i]! * x[xp[0]!]! * y[yp[0]!]! * z[zp[0]! - 1]!;
            }
        }
        return new Vector3Dd(rx, ry, rz);
    }

    public override evaluateField(point: Vector3Dd): number {
        return this.evaluate(point);
    }

    public override evaluateFieldGradient(point: Vector3Dd): Vector3Dd {
        return this.gradient(point);
    }

    public override encloseField(x: FloatingInterval, y: FloatingInterval, z: FloatingInterval): FloatingInterval {
        if (!x.isValid() || !y.isValid() || !z.isValid()) return new FloatingInterval();
        let result = FloatingInterval.exact(0.0);
        const xp: number[] = [0];
        const yp: number[] = [0];
        const zp: number[] = [0];
        const wp: number[] = [0];
        const P = PolynomialSurface.power;
        for (let i = 0; i < PolynomialSurface.TERM_COUNTS[this.order]!; i++) {
            if (this.coefficients[i]! === 0.0) continue;
            if (!Double.isFinite(this.coefficients[i]!)) return new FloatingInterval();
            PolynomialSurface.getTermPowers(this.order, i, xp, yp, zp, wp);
            result = result.add(
                FloatingInterval.exact(this.coefficients[i]!).multiply(
                    P(x, xp[0]!).multiply(P(y, yp[0]!)).multiply(P(z, zp[0]!)),
                ),
            );
        }
        return result;
    }

    public override encloseFieldGradient(
        x: FloatingInterval,
        y: FloatingInterval,
        z: FloatingInterval,
        gx: FloatingInterval[],
        gy: FloatingInterval[],
        gz: FloatingInterval[],
    ): boolean {
        if (!x.isValid() || !y.isValid() || !z.isValid()) return false;
        gx[0] = gy[0] = gz[0] = FloatingInterval.exact(0.0);
        const xp: number[] = [0];
        const yp: number[] = [0];
        const zp: number[] = [0];
        const wp: number[] = [0];
        const P = PolynomialSurface.power;
        for (let i = 0; i < PolynomialSurface.TERM_COUNTS[this.order]!; i++) {
            if (this.coefficients[i]! === 0.0) continue;
            if (!Double.isFinite(this.coefficients[i]!)) return false;
            PolynomialSurface.getTermPowers(this.order, i, xp, yp, zp, wp);
            const c = FloatingInterval.exact(this.coefficients[i]!);
            if (xp[0]! > 0)
                gx[0] = gx[0]!.add(
                    c.multiply(FloatingInterval.exact(xp[0]!)).multiply(
                        P(x, xp[0]! - 1)
                            .multiply(P(y, yp[0]!))
                            .multiply(P(z, zp[0]!)),
                    ),
                );
            if (yp[0]! > 0)
                gy[0] = gy[0]!.add(
                    c.multiply(FloatingInterval.exact(yp[0]!)).multiply(
                        P(x, xp[0]!)
                            .multiply(P(y, yp[0]! - 1))
                            .multiply(P(z, zp[0]!)),
                    ),
                );
            if (zp[0]! > 0)
                gz[0] = gz[0]!.add(
                    c.multiply(FloatingInterval.exact(zp[0]!)).multiply(
                        P(x, xp[0]!)
                            .multiply(P(y, yp[0]!))
                            .multiply(P(z, zp[0]! - 1)),
                    ),
                );
        }
        return gx[0]!.isValid() && gy[0]!.isValid() && gz[0]!.isValid();
    }

    public override findRayRoots(
        origin: Vector3Dd,
        unitDirection: Vector3Dd,
        _maximumDistance: number,
        distances: ArrayList<number>,
    ): boolean {
        for (let i = 0; i < PolynomialSurface.TERM_COUNTS[this.order]!; i++)
            if (!Double.isFinite(this.coefficients[i]!)) return false;
        const depths: number[] = new Array<number>(PolynomialSurface.MAXIMUM_ORDER).fill(0);
        // The typed queries keep every expanded coefficient.
        const count =
            this.order === 4
                ? this.intersectQuartic(origin, unitDirection, 0.0, depths)
                : this.intersectGeneral(origin, unitDirection, 0.0, depths, 0.0);
        for (let i = 0; i < count; i++) distances.add(depths[i]!);
        return true;
    }

    public override getBounds(result: GeometryBounds): void {
        for (let i = 0; i < PolynomialSurface.TERM_COUNTS[this.order]!; i++)
            if (!Double.isFinite(this.coefficients[i]!)) {
                result.setIndeterminate();
                return;
            }
        result.setUnbounded();
    }
}
