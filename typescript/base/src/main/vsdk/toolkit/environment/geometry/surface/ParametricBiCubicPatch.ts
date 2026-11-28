//= References:                                                             =
//= [WAYN1990] Knapp Wayne. "Ray with Bicubic Patch Intersection Problem",  =
//=            Ray Tracing News, volume 3, number 3, july 13 1990.          =
//=            available at                                                 =
//=         http://jedi.ks.uiuc.edu/~johns/raytracer/rtn/rtnv3n3.html#art19 =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics, princi-   =
//=            ples and practice" - second edition, Addison Wesley, 1992.   =

import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { Ray } from "../element/Ray.js";
import { VSDK } from "../../../common/VSDK.js";
import { ParametricCurve } from "../curve/ParametricCurve.js";
import type { RayHit } from "../element/RayHit.js";
import { QuadMesh } from "./QuadMesh.js";
import { Surface } from "./Surface.js";
import { _ParametricBiCubicPatchIntersector, type _PatchHit } from "./_ParametricBiCubicPatchIntersector.js";
import { Double } from "../../../../../java/lang/Double.js";
import { JavaArray } from "../../../../../java/lang/JavaArray.js";
import { NullPointerException } from "../../../../../java/lang/NullPointerException.js";
import { platformPrintln } from "../../../../../java/lang/_PlatformConsole.js";

export class ParametricBiCubicPatch extends Surface<Ray, RayHit> {
    public geometryMatrixX: Matrix4x4d = new Matrix4x4d();
    public geometryMatrixY: Matrix4x4d = new Matrix4x4d();
    public geometryMatrixZ: Matrix4x4d = new Matrix4x4d();

    private sParameterMatrix: Matrix4x4d | null;
    private tParameterMatrix: Matrix4x4d | null;
    private sDerivativeParameterMatrix: Matrix4x4d | null;
    private tDerivativeParameterMatrix: Matrix4x4d | null;
    private basisMatrix: Matrix4x4d | null;
    private transposedBasisMatrix: Matrix4x4d | null;
    private coefficientMatrixX: Matrix4x4d | null;
    private coefficientMatrixY: Matrix4x4d | null;
    private coefficientMatrixZ: Matrix4x4d | null;

    public static readonly FERGUSON = 7;

    /// Note that the contourCurve must have 4 points with its respective
    /// control parameters.
    public contourCurve: ParametricCurve | null;
    private controlMeshPoints: Vector3Dd[][] | null = null;
    public type: number;

    // Number of steps for curve approximation
    private static readonly INITIAL_APPROXIMATION_STEPS = 12;
    private approximationSteps: number;

    // Lazily built ray intersection structures, discarded each time the
    // patch matrices are rebuilt (Java `transient volatile`: the scene
    // snapshot transfer to the workers skips it).
    private rayIntersector: _ParametricBiCubicPatchIntersector | null = null;

    public constructor() {
        super();
        this.approximationSteps = ParametricBiCubicPatch.INITIAL_APPROXIMATION_STEPS;
        this.type = ParametricCurve.HERMITE;
        this.contourCurve = null;
        this.sParameterMatrix = null;
        this.tParameterMatrix = null;
        this.sDerivativeParameterMatrix = null;
        this.tDerivativeParameterMatrix = null;
        this.basisMatrix = null;
        this.transposedBasisMatrix = null;
        this.coefficientMatrixX = null;
        this.coefficientMatrixY = null;
        this.coefficientMatrixZ = null;
    }

    /**
    Given a contour curve, this constructor builds a corresponding
    FERGUSON type patch.

    PRE: Contour curve must has at least 4 control points. The first
    4 control points (the only one used) must be of type HERMITE.

    Note that a Ferguson patch is an Hermite patch with zero valued
    twist vectors.
    @param curve
    */
    public buildFergusonPatch(curve: ParametricCurve | null): void {
        this.contourCurve = curve;
        this.approximationSteps = ParametricBiCubicPatch.INITIAL_APPROXIMATION_STEPS;
        this.type = ParametricBiCubicPatch.FERGUSON;
        this.calculateMatrices();
    }

    public buildBezierPatch(controlMeshPoints: Vector3Dd[][] | null): void {
        this.controlMeshPoints = controlMeshPoints;
        this.approximationSteps = ParametricBiCubicPatch.INITIAL_APPROXIMATION_STEPS;
        this.type = ParametricCurve.BEZIER;
        this.calculateMatrices();
    }

    /**
    PRE: Some of the "build*Patch" methods should be called before calling
    this method.
    */
    private calculateMatrices(): void {
        //- Build matrices M, Gx, Gy, Gz and Mt ---------------------------
        if (this.type === ParametricCurve.BEZIER) {
            this.buildGeometryMatricesXYZ_Bezier();
            this.basisMatrix = ParametricCurve.BEZIER_MATRIX;
        } else if (this.type === ParametricCurve.HERMITE) {
            this.buildGeometryMatricesXYZ_Hermite();
            this.basisMatrix = ParametricCurve.HERMITE_MATRIX;
        } else if (this.type === ParametricBiCubicPatch.FERGUSON) {
            this.buildGeometryMatricesXYZ_Ferguson();
            this.basisMatrix = ParametricCurve.HERMITE_MATRIX;
        }
        const basisMatrix = ParametricBiCubicPatch.nonNull(this.basisMatrix);
        this.transposedBasisMatrix = new Matrix4x4d(basisMatrix);
        this.transposedBasisMatrix = this.transposedBasisMatrix.transpose();
        this.coefficientMatrixX = basisMatrix.multiply(this.geometryMatrixX).multiply(this.transposedBasisMatrix);
        this.coefficientMatrixY = basisMatrix.multiply(this.geometryMatrixY).multiply(this.transposedBasisMatrix);
        this.coefficientMatrixZ = basisMatrix.multiply(this.geometryMatrixZ).multiply(this.transposedBasisMatrix);
        this.sParameterMatrix = new Matrix4x4d();
        this.tParameterMatrix = new Matrix4x4d();
        this.sDerivativeParameterMatrix = new Matrix4x4d();
        this.tDerivativeParameterMatrix = new Matrix4x4d();
        this.rayIntersector = null;
    }

    /**
    Returns the ray intersection structures for current patch, building them
    on first use.
    @return intersector, or null if the patch has not been built yet
    */
    private getRayIntersector(): _ParametricBiCubicPatchIntersector | null {
        const intersector = this.rayIntersector;
        if (intersector !== null) {
            return intersector;
        }
        // Java synchronizes here; a JavaScript realm runs one thread.
        if (this.rayIntersector === null) {
            if (
                this.coefficientMatrixX === null ||
                this.coefficientMatrixY === null ||
                this.coefficientMatrixZ === null
            ) {
                return null;
            }
            this.rayIntersector = new _ParametricBiCubicPatchIntersector(
                this.coefficientMatrixX,
                this.coefficientMatrixY,
                this.coefficientMatrixZ,
            );
        }
        return this.rayIntersector;
    }

    /**
    Returns the control point of the Bezier patch equivalent to current
    patch (for Bezier patches, this is the original control mesh point).
    PRE: Some of the "build*Patch" methods should be called before calling
    this method.
    @param i control point index on the s direction, in [0, 3]
    @param j control point index on the t direction, in [0, 3]
    @return the Bezier control point (i, j), or null if the patch has not been
    built
    */
    public getBezierControlPoint(i: number, j: number): Vector3Dd | null {
        const intersector = this.getRayIntersector();
        if (intersector === null) {
            return null;
        }
        return intersector.getBezierControlPoint(i, j);
    }

    public getApproximationSteps(): number {
        return this.approximationSteps;
    }

    public setApproximationSteps(n: number): void {
        this.approximationSteps = n;
    }

    public getType(): number {
        return this.type;
    }

    public setType(type: number): void {
        this.type = type;
    }

    private static newMatrix(): number[][] {
        return [
            [0, 0, 0, 0],
            [0, 0, 0, 0],
            [0, 0, 0, 0],
            [0, 0, 0, 0],
        ];
    }

    private buildGeometryMatricesXYZ_Bezier(): void {
        const mx = ParametricBiCubicPatch.newMatrix();
        const my = ParametricBiCubicPatch.newMatrix();
        const mz = ParametricBiCubicPatch.newMatrix();
        let i: number;
        let j: number;

        for (i = 0; i < 4; i++) {
            for (j = 0; j < 4; j++) {
                const vp = ParametricBiCubicPatch.nonNull(
                    JavaArray.get(JavaArray.get(this.controlMeshPoints, i), j) ?? null,
                );
                mx[i]![j] = vp.x();
                my[i]![j] = vp.y();
                mz[i]![j] = vp.z();
            }
        }
        this.geometryMatrixX = Matrix4x4d.copyOf(mx);
        this.geometryMatrixY = Matrix4x4d.copyOf(my);
        this.geometryMatrixZ = Matrix4x4d.copyOf(mz);
        //printGeometryMatrices();
    }

    /** `contourCurve.getPoint(p)` with Java's null checks. */
    private contourPoint(p: number): Vector3Dd[] {
        return ParametricBiCubicPatch.nonNull(ParametricBiCubicPatch.nonNull(this.contourCurve).getPoint(p));
    }

    /** `vp[k]` of a contour point, with Java's checks. */
    private static pointAt(vp: Vector3Dd[], k: number): Vector3Dd {
        return ParametricBiCubicPatch.nonNull(JavaArray.get(vp, k) ?? null);
    }

    private buildGeometryMatricesXYZ_Hermite(): void {
        const mx = ParametricBiCubicPatch.newMatrix();
        const my = ParametricBiCubicPatch.newMatrix();
        const mz = ParametricBiCubicPatch.newMatrix();
        const at = ParametricBiCubicPatch.pointAt;

        /* The upper-left 2x2 matrix portion of Gh contains the x coordinates
           of the four corners of the patch.
           The upper-right and lower-left 2x2 matrix portion of Gh contains
           the x coordiates of the tangent vectors four corners of the patch.
        */
        let p = 0;
        let i = 0;
        for (let j = 0; j < 2; j++) {
            const vp = this.contourPoint(p);

            mx[i]![j] = at(vp, 0).x();
            my[i]![j] = at(vp, 0).y();
            mz[i]![j] = at(vp, 0).z();

            mx[i]![j + 2] = at(vp, 2 - j).x();
            my[i]![j + 2] = at(vp, 2 - j).y();
            mz[i]![j + 2] = at(vp, 2 - j).z();

            mx[i + 2]![j] = at(vp, 1 + j).x();
            my[i + 2]![j] = at(vp, 1 + j).y();
            mz[i + 2]![j] = at(vp, 1 + j).z();

            const vn = new Vector3Dd(0, 0, 0);

            mx[i + 2]![j + 2] = vn.x();
            my[i + 2]![j + 2] = vn.y();
            mz[i + 2]![j + 2] = vn.z();
            p++;
        }
        p = 2;
        i = 1;
        for (let j = 0; j < 2; j++) {
            const vp = this.contourPoint(p);

            mx[i]![j] = at(vp, 0).x();
            my[i]![j] = at(vp, 0).y();
            mz[i]![j] = at(vp, 0).z();

            mx[i]![j + 2] = at(vp, j + 1).x();
            my[i]![j + 2] = at(vp, j + 1).y();
            mz[i]![j + 2] = at(vp, j + 1).z();

            mx[i + 2]![j] = at(vp, 2 - j).x();
            my[i + 2]![j] = at(vp, 2 - j).y();
            mz[i + 2]![j] = at(vp, 2 - j).z();

            const vn = new Vector3Dd(0, 0, 0);

            mx[i + 2]![j + 2] = vn.x();
            my[i + 2]![j + 2] = vn.y();
            mz[i + 2]![j + 2] = vn.z();
            p++;
        }

        this.geometryMatrixX = Matrix4x4d.copyOf(mx);
        this.geometryMatrixY = Matrix4x4d.copyOf(my);
        this.geometryMatrixZ = Matrix4x4d.copyOf(mz);
    }

    public printGeometryMatrices(): void {
        const mx = this.geometryMatrixX.toArrayCopy();
        const my = this.geometryMatrixY.toArrayCopy();
        const mz = this.geometryMatrixZ.toArrayCopy();

        platformPrintln(
            "[ <" +
                VSDK.formatDouble(mx[0]![0]!) +
                ", " +
                VSDK.formatDouble(my[0]![0]!) +
                ", " +
                VSDK.formatDouble(mz[0]![0]!) +
                "> | <" +
                VSDK.formatDouble(mx[0]![1]!) +
                ", " +
                VSDK.formatDouble(my[0]![1]!) +
                ", " +
                VSDK.formatDouble(mz[0]![1]!) +
                "> | <" +
                VSDK.formatDouble(mx[0]![2]!) +
                ", " +
                VSDK.formatDouble(my[0]![2]!) +
                ", " +
                VSDK.formatDouble(mz[0]![2]!) +
                "> | <" +
                VSDK.formatDouble(mx[0]![3]!) +
                ", " +
                VSDK.formatDouble(my[0]![3]!) +
                ", " +
                VSDK.formatDouble(mz[0]![3]!) +
                "> ]",
        );

        platformPrintln(
            "[ <" +
                VSDK.formatDouble(mx[1]![0]!) +
                ", " +
                VSDK.formatDouble(my[1]![0]!) +
                ", " +
                VSDK.formatDouble(mz[1]![0]!) +
                "> | <" +
                VSDK.formatDouble(mx[1]![1]!) +
                ", " +
                VSDK.formatDouble(my[1]![1]!) +
                ", " +
                VSDK.formatDouble(mz[1]![1]!) +
                "> | <" +
                VSDK.formatDouble(mx[1]![2]!) +
                ", " +
                VSDK.formatDouble(my[1]![2]!) +
                ", " +
                VSDK.formatDouble(mz[1]![2]!) +
                "> | <" +
                VSDK.formatDouble(mx[1]![3]!) +
                ", " +
                VSDK.formatDouble(my[1]![3]!) +
                ", " +
                VSDK.formatDouble(mz[1]![3]!) +
                "> ]",
        );

        platformPrintln(
            "[ <" +
                VSDK.formatDouble(mx[2]![0]!) +
                ", " +
                VSDK.formatDouble(my[2]![0]!) +
                ", " +
                VSDK.formatDouble(mz[2]![0]!) +
                "> | <" +
                VSDK.formatDouble(mx[2]![1]!) +
                ", " +
                VSDK.formatDouble(my[2]![1]!) +
                ", " +
                VSDK.formatDouble(mz[2]![1]!) +
                "> | <" +
                VSDK.formatDouble(mx[2]![2]!) +
                ", " +
                VSDK.formatDouble(my[2]![2]!) +
                ", " +
                VSDK.formatDouble(mz[2]![2]!) +
                "> | <" +
                VSDK.formatDouble(mx[2]![3]!) +
                ", " +
                VSDK.formatDouble(my[2]![3]!) +
                ", " +
                VSDK.formatDouble(mz[2]![3]!) +
                "> ]",
        );

        platformPrintln(
            "[ <" +
                VSDK.formatDouble(mx[3]![0]!) +
                ", " +
                VSDK.formatDouble(my[3]![0]!) +
                ", " +
                VSDK.formatDouble(mz[3]![0]!) +
                "> | <" +
                VSDK.formatDouble(mx[3]![1]!) +
                ", " +
                VSDK.formatDouble(my[3]![1]!) +
                ", " +
                VSDK.formatDouble(mz[3]![1]!) +
                "> | <" +
                VSDK.formatDouble(mx[3]![2]!) +
                ", " +
                VSDK.formatDouble(my[3]![2]!) +
                ", " +
                VSDK.formatDouble(mz[3]![2]!) +
                "> | <" +
                VSDK.formatDouble(mx[3]![3]!) +
                ", " +
                VSDK.formatDouble(my[3]![3]!) +
                ", " +
                VSDK.formatDouble(mz[3]![3]!) +
                "> ]",
        );
    }

    /**
    \todo  verify that current contour curve have at least 4 control points
    and that first 4 control points are of type HERMITE.
    */
    private buildGeometryMatricesXYZ_Ferguson(): void {
        const mx = ParametricBiCubicPatch.newMatrix();
        const my = ParametricBiCubicPatch.newMatrix();
        const mz = ParametricBiCubicPatch.newMatrix();
        const at = ParametricBiCubicPatch.pointAt;
        const vp00 = this.contourPoint(0);
        const vp10 = this.contourPoint(1);
        const vp11 = this.contourPoint(2);
        const vp01 = this.contourPoint(3);

        // Positions with respect to contour curve
        mx[0]![0] = at(vp00, 0).x();
        my[0]![0] = at(vp00, 0).y();
        mz[0]![0] = at(vp00, 0).z();
        mx[0]![1] = at(vp01, 0).x();
        my[0]![1] = at(vp01, 0).y();
        mz[0]![1] = at(vp01, 0).z();
        mx[1]![0] = at(vp10, 0).x();
        my[1]![0] = at(vp10, 0).y();
        mz[1]![0] = at(vp10, 0).z();
        mx[1]![1] = at(vp11, 0).x();
        my[1]![1] = at(vp11, 0).y();
        mz[1]![1] = at(vp11, 0).z();

        // Partial derivatives with respect to S direction
        // For Hermite contour
        mx[2]![0] = at(vp00, 2).x();
        my[2]![0] = at(vp00, 2).y();
        mz[2]![0] = at(vp00, 2).z();
        mx[2]![1] = -at(vp01, 1).x();
        my[2]![1] = -at(vp01, 1).y();
        mz[2]![1] = -at(vp01, 1).z();
        mx[3]![0] = at(vp10, 1).x();
        my[3]![0] = at(vp10, 1).y();
        mz[3]![0] = at(vp10, 1).z();
        mx[3]![1] = -at(vp11, 2).x();
        my[3]![1] = -at(vp11, 2).y();
        mz[3]![1] = -at(vp11, 2).z();

        // Partial derivatives with respect to T direction
        // For Hermite contour
        mx[0]![2] = -at(vp00, 1).x();
        my[0]![2] = -at(vp00, 1).y();
        mz[0]![2] = -at(vp00, 1).z();
        mx[0]![3] = -at(vp01, 2).x();
        my[0]![3] = -at(vp01, 2).y();
        mz[0]![3] = -at(vp01, 2).z();
        mx[1]![2] = at(vp10, 2).x();
        my[1]![2] = at(vp10, 2).y();
        mz[1]![2] = at(vp10, 2).z();
        mx[1]![3] = at(vp11, 1).x();
        my[1]![3] = at(vp11, 1).y();
        mz[1]![3] = at(vp11, 1).z();

        // Ferguson patch: twist vectors (second order partial derivatives)
        // are all 0, as noted on [FOLE1992].11.3.1, equation [FOLE1992].11.84
        mx[2]![2] = 0;
        my[2]![2] = 0;
        mz[2]![2] = 0;
        mx[2]![3] = 0;
        my[2]![3] = 0;
        mz[2]![3] = 0;
        mx[3]![2] = 0;
        my[3]![2] = 0;
        mz[3]![2] = 0;
        mx[3]![3] = 0;
        my[3]![3] = 0;
        mz[3]![3] = 0;

        // Final result
        this.geometryMatrixX = Matrix4x4d.copyOf(mx);
        this.geometryMatrixY = Matrix4x4d.copyOf(my);
        this.geometryMatrixZ = Matrix4x4d.copyOf(mz);
        //printGeometryMatrices();
    }

    /**
    This method evaluates current patch position in the parameter space
    position (s, t), computing the equation set 11.76 in [FOLE1992].

    The following class attributes are used:
    <UL>
      <LI> sParameterMatrix  Column vector for storing s parameter polynomial as
      explain in section [FOLE1992].11.3
      <LI> tParameterMatrix Row vector for storing t parameter polynomial
      <LI> basisMatrix  Patch's blending function
      <LI> transposedBasisMatrix M's transpose
      <LI> geometryMatrixX Geometry matrix for x
      <LI> geometryMatrixY Geometry matrix for y
      <LI> geometryMatrixZ Geometry matrix for z
    </UL>

    PRE: calculateMAtrices() should be called before calling this method.

    Java also keeps the deprecated `evaluate(Vector3Dd p, double s, double t)`,
    the former output parameter form: `Vector3Dd` is immutable, so the given
    vector can not receive the point and the returned one must be used.
    @param s parameter in the first direction, in [0, 1]
    @param t parameter in the second direction, in [0, 1]
    @return the point of the patch at (s, t)
    */
    public evaluate(s: number, t: number): Vector3Dd;
    /** @deprecated use `evaluate(s, t)` */
    public evaluate(p: Vector3Dd | null, s: number, t: number): Vector3Dd;
    public evaluate(a: number | Vector3Dd | null, b: number, c?: number): Vector3Dd {
        if (c !== undefined) {
            return this.evaluate(b, c);
        }
        const s = a as number;
        const t = b;
        this.sParameterMatrix = ParametricBiCubicPatch.nonNull(this.sParameterMatrix)
            .withVal(0, 0, s * s * s)
            .withVal(0, 1, s * s)
            .withVal(0, 2, s)
            .withVal(0, 3, 1);

        this.tParameterMatrix = ParametricBiCubicPatch.nonNull(this.tParameterMatrix)
            .withVal(0, 0, t * t * t)
            .withVal(1, 0, t * t)
            .withVal(2, 0, t)
            .withVal(3, 0, 1);

        const S_M_Gx_Mt_MATRIX = this.sParameterMatrix.multiply(this.coefficientMatrixX!);
        const S_M_Gy_Mt_MATRIX = this.sParameterMatrix.multiply(this.coefficientMatrixY!);
        const S_M_Gz_Mt_MATRIX = this.sParameterMatrix.multiply(this.coefficientMatrixZ!);
        const Qx_MATRIX = S_M_Gx_Mt_MATRIX.multiply(this.tParameterMatrix);
        const Qy_MATRIX = S_M_Gy_Mt_MATRIX.multiply(this.tParameterMatrix);
        const Qz_MATRIX = S_M_Gz_Mt_MATRIX.multiply(this.tParameterMatrix);

        // The result is a 1x1 matrix.
        return new Vector3Dd(Qx_MATRIX.get(0, 0), Qy_MATRIX.get(0, 0), Qz_MATRIX.get(0, 0));
    }

    public evaluateTangent(s: number, t: number): Vector3Dd {
        this.sDerivativeParameterMatrix = ParametricBiCubicPatch.nonNull(this.sDerivativeParameterMatrix)
            .withVal(0, 0, 3 * s * s)
            .withVal(0, 1, 2 * s)
            .withVal(0, 2, 1)
            .withVal(0, 3, 0);
        this.tParameterMatrix = ParametricBiCubicPatch.nonNull(this.tParameterMatrix)
            .withVal(0, 0, t * t * t)
            .withVal(1, 0, t * t)
            .withVal(2, 0, t)
            .withVal(3, 0, 1);

        const S_M_Gx_Mt_MATRIX = this.sDerivativeParameterMatrix.multiply(this.coefficientMatrixX!);
        const S_M_Gy_Mt_MATRIX = this.sDerivativeParameterMatrix.multiply(this.coefficientMatrixY!);
        const S_M_Gz_Mt_MATRIX = this.sDerivativeParameterMatrix.multiply(this.coefficientMatrixZ!);
        const Qx_MATRIX = S_M_Gx_Mt_MATRIX.multiply(this.tParameterMatrix);
        const Qy_MATRIX = S_M_Gy_Mt_MATRIX.multiply(this.tParameterMatrix);
        const Qz_MATRIX = S_M_Gz_Mt_MATRIX.multiply(this.tParameterMatrix);

        // The result is a 1x1 matrix.
        let result = new Vector3Dd();

        result = new Vector3Dd(Qx_MATRIX.get(0, 0), Qy_MATRIX.get(0, 0), Qz_MATRIX.get(0, 0));
        result = result.normalized();

        return result;
    }

    public evaluateBinormal(s: number, t: number): Vector3Dd {
        this.sParameterMatrix = ParametricBiCubicPatch.nonNull(this.sParameterMatrix)
            .withVal(0, 0, s * s * s)
            .withVal(0, 1, s * s)
            .withVal(0, 2, s)
            .withVal(0, 3, 1);

        this.tDerivativeParameterMatrix = ParametricBiCubicPatch.nonNull(this.tDerivativeParameterMatrix)
            .withVal(0, 0, 3 * t * t)
            .withVal(1, 0, 2 * t)
            .withVal(2, 0, 1)
            .withVal(3, 0, 0);

        const S_M_Gx_Mt_MATRIX = this.sParameterMatrix.multiply(this.coefficientMatrixX!);
        const S_M_Gy_Mt_MATRIX = this.sParameterMatrix.multiply(this.coefficientMatrixY!);
        const S_M_Gz_Mt_MATRIX = this.sParameterMatrix.multiply(this.coefficientMatrixZ!);
        const Qx_MATRIX = S_M_Gx_Mt_MATRIX.multiply(this.tDerivativeParameterMatrix);
        const Qy_MATRIX = S_M_Gy_Mt_MATRIX.multiply(this.tDerivativeParameterMatrix);
        const Qz_MATRIX = S_M_Gz_Mt_MATRIX.multiply(this.tDerivativeParameterMatrix);

        // The results are 1x1 matrices.
        let result = new Vector3Dd();
        result = new Vector3Dd(Qx_MATRIX.get(0, 0), Qy_MATRIX.get(0, 0), Qz_MATRIX.get(0, 0));
        result = result.normalized();

        return result;
    }

    /**
    This method evaluates current patch gradient in the parameter space
    position (s, t), computing the patch normal as explain in section
    [FOLE1992].11.3.4.

    PRE: calculateMAtrices() should be called before calling this method.
    */
    public evaluateNormal(s: number, t: number): Vector3Dd {
        let dQds: Vector3Dd;
        let dQdt: Vector3Dd;

        dQds = this.evaluateTangent(s, t);
        dQdt = this.evaluateBinormal(s, t);

        let nn = dQds.crossProduct(dQdt);
        nn = nn.normalized();

        return nn;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersectionFirstHit.

    Check the discusion in [WAYN1990] about solving this problem. This
    implementation follows the subdivision strategy of POV-Ray's
    bicubic_patch (as replicated on povCpp): the patch is converted to Bezier
    form and recursively subdivided into nearly flat sub-patches, organized on
    a tree of bounding spheres. Hits against the flat sub-patches are then
    refined with Newton iterations over the exact surface.

    The one argument form (Java's `doIntersectionFirstHit(Ray r)`) returns a
    copy of given ray with its t set at the nearest intersection, or null if
    the ray misses the patch.
    */
    public doIntersectionFirstHit(r: Ray | null): Ray | null;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit): boolean;
    public override doIntersectionFirstHit(inRay: Ray | null, outHit: RayHit | null): boolean;
    public override doIntersectionFirstHit(inRay: Ray | null, outHit?: RayHit | null): Ray | null | boolean {
        const intersector = this.getRayIntersector();
        if (outHit === undefined) {
            if (inRay === null || intersector === null) {
                return null;
            }
            const hit = intersector.intersect(inRay);
            if (hit === null) {
                return null;
            }
            return inRay.withT(hit.t);
        }
        if (inRay === null || intersector === null) {
            return false;
        }
        const hit = intersector.intersect(inRay);
        if (hit === null) {
            return false;
        }

        if (outHit !== null) {
            if (outHit.shouldStoreRay() || outHit.needsAnySurfaceData()) {
                const hitRay = inRay.withT(hit.t);
                outHit.setRay(hitRay);
                if (outHit.needsAnySurfaceData()) {
                    ParametricBiCubicPatch.fillHitInformation(intersector, hit, inRay, outHit);
                }
            } else {
                outHit.setHitDistance(hit.t);
            }
        }
        return true;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.

    Since a patch surface point can not be mapped back to its (s, t)
    parameters in closed form, the ray is intersected again and the
    information reported is the one for the nearest hit.
    @param inRay ray that intersects current patch
    @param intT ray parameter at the intersection point
    @param outData receives the intersection details
    */
    public override doExtraInformation(inRay: Ray, intT: number, outData: RayHit): void {
        const intersector = this.getRayIntersector();
        if ((inRay as Ray | null) === null || (outData as RayHit | null) === null || intersector === null) {
            return;
        }
        const hit = intersector.intersect(inRay);
        if (hit === null) {
            if (outData.needsPoint()) {
                outData.point = new Vector3Dd(
                    inRay.getOrigin().x() + intT * inRay.getDirection().x(),
                    inRay.getOrigin().y() + intT * inRay.getDirection().y(),
                    inRay.getOrigin().z() + intT * inRay.getDirection().z(),
                );
            }
            return;
        }
        ParametricBiCubicPatch.fillHitInformation(intersector, hit, inRay, outData);
    }

    /**
    Fills point, normal, tangent and (s, t) texture coordinates for a hit.
    The normal is oriented against the ray direction, as the patch is an
    open surface.
    */
    private static fillHitInformation(
        intersector: _ParametricBiCubicPatchIntersector,
        hit: _PatchHit,
        inRay: Ray,
        outData: RayHit,
    ): void {
        if (outData.needsPoint()) {
            outData.point = new Vector3Dd(hit.pointX, hit.pointY, hit.pointZ);
        }
        if (!outData.needsNormal() && !outData.needsTangent() && !outData.needsTextureCoordinates()) {
            return;
        }

        const derivatives = new Float64Array(9);
        intersector.evaluate(hit.u, hit.v, derivatives);
        const dQds = new Vector3Dd(derivatives[3]!, derivatives[4]!, derivatives[5]!);
        const dQdt = new Vector3Dd(derivatives[6]!, derivatives[7]!, derivatives[8]!);
        const triangleNormal = new Vector3Dd(hit.triangleNx, hit.triangleNy, hit.triangleNz);

        let normal = dQds.crossProduct(dQdt);
        if (normal.length() <= VSDK.EPSILON * VSDK.EPSILON) {
            // Degenerated parameterization (i.e. collapsed patch border)
            normal = triangleNormal;
        }
        normal = normal.normalized();
        if (normal.dotProduct(inRay.getDirection()) > 0) {
            normal = normal.multiply(-1);
        }

        if (outData.needsNormal()) {
            outData.normal = normal;
        }
        if (outData.needsTangent()) {
            let tangent = dQds;
            if (tangent.length() <= VSDK.EPSILON * VSDK.EPSILON) {
                tangent = dQdt;
            }
            outData.tangent = tangent.normalized();
        }
        if (outData.needsTextureCoordinates()) {
            outData.u = hit.u;
            outData.v = hit.v;
        }
    }

    /**
    Returns a bounding volume minmax for current patch. When the patch has
    been built, this is the minmax of its equivalent Bezier control net, which
    contains the whole patch by the convex hull property.
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    public override getMinMax(): number[] | Float64Array {
        const intersector = this.getRayIntersector();
        if (intersector !== null) {
            return intersector.getMinMax();
        } else if (this.contourCurve !== null) {
            return this.contourCurve.getMinMax();
        } else {
            // This gives convex hull's minmax
            let minX = Double.MAX_VALUE;
            let minY = Double.MAX_VALUE;
            let minZ = Double.MAX_VALUE;
            let maxX = -Double.MAX_VALUE;
            let maxY = -Double.MAX_VALUE;
            let maxZ = -Double.MAX_VALUE;
            const minMax: number[] = [0, 0, 0, 0, 0, 0];
            let i: number, j: number;

            for (i = 0; i < 4; i++) {
                for (j = 0; j < 4; j++) {
                    const p = ParametricBiCubicPatch.nonNull(
                        JavaArray.get(JavaArray.get(this.controlMeshPoints, i), j) ?? null,
                    );

                    if (p.x() < minX) minX = p.x();
                    if (p.y() < minY) minY = p.y();
                    if (p.z() < minZ) minZ = p.z();
                    if (p.x() > maxX) maxX = p.x();
                    if (p.y() > maxY) maxY = p.y();
                    if (p.z() > maxZ) maxZ = p.z();
                }
            }
            minMax[0] = minX;
            minMax[1] = minY;
            minMax[2] = minZ;
            minMax[3] = maxX;
            minMax[4] = maxY;
            minMax[5] = maxZ;
            return minMax;
        }
    }

    /**
    Exports an approximation of the patch as a mesh of quadrilaterals: the
    patch is evaluated on a grid of `getApproximationSteps()` x
    `getApproximationSteps()` cells of its parameters (s, t) in [0, 1] x
    [0, 1], so the lines of the mesh are isoparametric lines of the patch.
    @return a new mesh
    */
    public exportToQuadMesh(): QuadMesh {
        const steps = Math.max(1, this.getApproximationSteps());
        const n = steps + 1;
        const mesh = new QuadMesh();
        let i: number;
        let j: number;
        let index: number;

        mesh.initVertexPositionsArray(n * n);
        const positions = mesh.getVertexPositions()!;
        for (i = 0; i < n; i++) {
            const s = i / steps;
            for (j = 0; j < n; j++) {
                const p = this.evaluate(s, j / steps);
                index = i * n + j;
                positions[3 * index] = p.x();
                positions[3 * index + 1] = p.y();
                positions[3 * index + 2] = p.z();
            }
        }
        mesh.initQuadArrays(steps * steps);
        index = 0;
        for (i = 0; i < steps; i++) {
            for (j = 0; j < steps; j++) {
                mesh.setQuadAt(index, i * n + j, (i + 1) * n + j, (i + 1) * n + j + 1, i * n + j + 1);
                index++;
            }
        }
        return mesh;
    }

    private static nonNull<T>(value: T | null): T {
        if (value === null) {
            throw new NullPointerException();
        }
        return value;
    }
}
