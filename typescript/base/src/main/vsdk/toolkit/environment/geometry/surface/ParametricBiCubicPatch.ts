import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { ParametricCurve } from "../curve/ParametricCurve.js";
import { Ray } from "../element/Ray.js";
import { RayHit } from "../element/RayHit.js";
import { Surface } from "./Surface.js";
import { VSDK } from "../../../common/VSDK.js";

/** Tensor-product cubic patch using the Vitral curve blending matrices. */
export class ParametricBiCubicPatch extends Surface<Ray, RayHit> {
    public static readonly FERGUSON = 7;
    public Gx_MATRIX = new Matrix4x4d();
    public Gy_MATRIX = new Matrix4x4d();
    public Gz_MATRIX = new Matrix4x4d();
    public contourCurve: ParametricCurve | null = null;
    public type = ParametricCurve.HERMITE;
    private controlMeshPoints: Vector3Dd[][] | null = null;
    private approximationSteps = 12;
    private coefficients: [Matrix4x4d, Matrix4x4d, Matrix4x4d] | null = null;

    public buildFergusonPatch(curve: ParametricCurve): void {
        this.contourCurve = curve;
        this.controlMeshPoints = null;
        this.type = ParametricBiCubicPatch.FERGUSON;
        this.approximationSteps = 12;
        this.calculateMatrices();
    }

    public buildBezierPatch(points: Vector3Dd[][]): void {
        this.assertMesh(points);
        this.controlMeshPoints = points.map((row) => row.map((point) => new Vector3Dd(point)));
        this.contourCurve = null;
        this.type = ParametricCurve.BEZIER;
        this.approximationSteps = 12;
        this.calculateMatrices();
    }

    public getApproximationSteps(): number {
        return this.approximationSteps;
    }
    public setApproximationSteps(steps: number): void {
        this.approximationSteps = steps;
    }
    public getType(): number {
        return this.type;
    }
    public setType(type: number): void {
        this.type = type;
    }

    /** Prints the three geometry matrices in Java's row-oriented diagnostic format. */
    public printGeometryMatrices(): void {
        const x = this.Gx_MATRIX.toArrayCopy(),
            y = this.Gy_MATRIX.toArrayCopy(),
            z = this.Gz_MATRIX.toArrayCopy();
        for (let row = 0; row < 4; row++) {
            const values: string[] = [];
            for (let column = 0; column < 4; column++)
                values.push(
                    `<${VSDK.formatDouble(x[row]![column]!)}, ${VSDK.formatDouble(y[row]![column]!)}, ${VSDK.formatDouble(z[row]![column]!)}>`,
                );
            console.log(`[ ${values.join(" | ")} ]`);
        }
    }

    public evaluate(s: number, t: number): Vector3Dd;
    public evaluate(_out: Vector3Dd, s: number, t: number): Vector3Dd;
    public evaluate(a: number | Vector3Dd, b: number, c?: number): Vector3Dd {
        const [s, t] = typeof a === "number" ? [a, b] : [b, c!];
        const [x, y, z] = this.evaluateComponents(s, t, false, false);
        return new Vector3Dd(x, y, z);
    }

    public evaluateTangent(s: number, t: number): Vector3Dd {
        const [x, y, z] = this.evaluateComponents(s, t, true, false);
        return new Vector3Dd(x, y, z).normalized();
    }

    public evaluateBinormal(s: number, t: number): Vector3Dd {
        const [x, y, z] = this.evaluateComponents(s, t, false, true);
        return new Vector3Dd(x, y, z).normalized();
    }

    public evaluateNormal(s: number, t: number): Vector3Dd {
        return this.evaluateTangent(s, t).crossProduct(this.evaluateBinormal(s, t)).normalized();
    }

    /** Java left this intersection routine unimplemented; retain that contract. */
    public doIntersectionFirstHit(_inRay: Ray, _outHit: RayHit): boolean {
        return false;
    }

    public getMinMax(): Float64Array {
        if (this.contourCurve !== null) return this.contourCurve.getMinMax();
        if (this.controlMeshPoints === null)
            return new Float64Array([Infinity, Infinity, Infinity, -Infinity, -Infinity, -Infinity]);
        const bounds = new Float64Array([Infinity, Infinity, Infinity, -Infinity, -Infinity, -Infinity]);
        for (const row of this.controlMeshPoints)
            for (const point of row) {
                bounds[0] = Math.min(bounds[0]!, point.x());
                bounds[1] = Math.min(bounds[1]!, point.y());
                bounds[2] = Math.min(bounds[2]!, point.z());
                bounds[3] = Math.max(bounds[3]!, point.x());
                bounds[4] = Math.max(bounds[4]!, point.y());
                bounds[5] = Math.max(bounds[5]!, point.z());
            }
        return bounds;
    }

    private calculateMatrices(): void {
        let blending: Matrix4x4d;
        if (this.type === ParametricCurve.BEZIER) {
            this.buildBezierGeometry();
            blending = ParametricCurve.BEZIER_MATRIX;
        } else if (this.type === ParametricBiCubicPatch.FERGUSON) {
            this.buildFergusonGeometry();
            blending = ParametricCurve.HERMITE_MATRIX;
        } else if (this.type === ParametricCurve.HERMITE) {
            this.buildHermiteGeometry();
            blending = ParametricCurve.HERMITE_MATRIX;
        } else throw new RangeError(`Unsupported bicubic patch type: ${this.type}`);
        const transpose = blending.transpose();
        this.coefficients = [
            blending.multiply(this.Gx_MATRIX).multiply(transpose),
            blending.multiply(this.Gy_MATRIX).multiply(transpose),
            blending.multiply(this.Gz_MATRIX).multiply(transpose),
        ];
    }

    private buildBezierGeometry(): void {
        this.setGeometry(this.controlMeshPoints!);
    }
    private buildHermiteGeometry(): void {
        const curve = this.requireContour();
        const mesh = Array.from({ length: 4 }, () => Array.from({ length: 4 }, () => new Vector3Dd()));
        let p = 0;
        for (let j = 0; j < 2; j++, p++) {
            const v = this.requirePoint(curve, p);
            mesh[0]![j] = v[0]!;
            mesh[0]![j + 2] = v[2 - j]!;
            mesh[2]![j] = v[1 + j]!;
        }
        p = 2;
        for (let j = 0; j < 2; j++, p++) {
            const v = this.requirePoint(curve, p);
            mesh[1]![j] = v[0]!;
            mesh[1]![j + 2] = v[j + 1]!;
            mesh[3]![j] = v[2 - j]!;
        }
        this.setGeometry(mesh);
    }
    private buildFergusonGeometry(): void {
        const curve = this.requireContour();
        const p00 = this.requirePoint(curve, 0),
            p10 = this.requirePoint(curve, 1),
            p11 = this.requirePoint(curve, 2),
            p01 = this.requirePoint(curve, 3);
        const mesh = Array.from({ length: 4 }, () => Array.from({ length: 4 }, () => new Vector3Dd()));
        mesh[0]![0] = p00[0]!;
        mesh[0]![1] = p01[0]!;
        mesh[1]![0] = p10[0]!;
        mesh[1]![1] = p11[0]!;
        mesh[2]![0] = p00[2]!;
        mesh[2]![1] = p01[1]!.multiply(-1);
        mesh[3]![0] = p10[1]!;
        mesh[3]![1] = p11[2]!.multiply(-1);
        mesh[0]![2] = p00[1]!.multiply(-1);
        mesh[0]![3] = p01[2]!.multiply(-1);
        mesh[1]![2] = p10[2]!;
        mesh[1]![3] = p11[1]!;
        this.setGeometry(mesh);
    }
    private setGeometry(mesh: Vector3Dd[][]): void {
        this.assertMesh(mesh);
        const xyz = [0, 1, 2].map((axis) =>
            mesh.map((row) => row.map((p) => (axis === 0 ? p.x() : axis === 1 ? p.y() : p.z()))),
        );
        this.Gx_MATRIX = Matrix4x4d.copyOf(xyz[0]!);
        this.Gy_MATRIX = Matrix4x4d.copyOf(xyz[1]!);
        this.Gz_MATRIX = Matrix4x4d.copyOf(xyz[2]!);
    }
    private evaluateComponents(s: number, t: number, ds: boolean, dt: boolean): [number, number, number] {
        if (this.coefficients === null) throw new Error("Build a bicubic patch before evaluating it");
        const sv = ds ? [3 * s * s, 2 * s, 1, 0] : [s * s * s, s * s, s, 1];
        const tv = dt ? [3 * t * t, 2 * t, 1, 0] : [t * t * t, t * t, t, 1];
        return this.coefficients.map((matrix) => {
            let value = 0;
            for (let i = 0; i < 4; i++) for (let j = 0; j < 4; j++) value += sv[i]! * matrix.get(i, j) * tv[j]!;
            return value;
        }) as [number, number, number];
    }
    private requireContour(): ParametricCurve {
        if (this.contourCurve === null) throw new Error("A contour curve is required for this patch type");
        return this.contourCurve;
    }
    private requirePoint(curve: ParametricCurve, index: number): Vector3Dd[] {
        const point = curve.getPoint(index);
        if (
            point === null ||
            point.length < 3 ||
            point[0] === undefined ||
            point[1] === undefined ||
            point[2] === undefined
        )
            throw new RangeError("Bicubic contour requires four complete control points");
        return point as Vector3Dd[];
    }
    private assertMesh(mesh: Vector3Dd[][]): void {
        if (
            mesh.length !== 4 ||
            mesh.some((row) => row.length !== 4 || row.some((point) => !(point instanceof Vector3Dd)))
        )
            throw new RangeError("A bicubic Bezier patch requires a 4 by 4 control mesh");
    }
}
